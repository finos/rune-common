package com.regnosys.rosetta.common.serialisation.xml.serialization;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanSerializer;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap;
import com.regnosys.rosetta.common.serialisation.xml.VirtualXMLAttribute;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Add a `schemaLocation` to the root XML element, if configured.
 * It can be configured through an object writer by using
 * {@code withAttribute}. Example:
 *
 * <pre>
 * xmlMapper
 *     .writerWithDefaultPrettyPrinter()
 *     .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd")
 * </pre>
 */
public class RosettaBeanSerializer extends XmlBeanSerializer {
    public static final String SCHEMA_LOCATION_ATTRIBUTE_NAME = "schemaLocation";
    private static final String SCHEMA_LOCATION_ATTRIBUTE_PREFIXED_NAME = "xsi:" + SCHEMA_LOCATION_ATTRIBUTE_NAME;

    private final SubstitutionMap _substitutionMap;

    // When present, child elements are serialised in the order dictated by the XSD content model
    // (computed per object against the present data), rather than the default property order.
    private final XMLContentModelOrderer _contentModelOrderer;

    public RosettaBeanSerializer(XmlBeanSerializer src, SubstitutionMap substitutionMap) {
        this(src, substitutionMap, null);
    }

    public RosettaBeanSerializer(XmlBeanSerializer src, SubstitutionMap substitutionMap,
                                 XMLContentModelOrderer contentModelOrderer) {
        super(src);
        this._substitutionMap = substitutionMap;
        this._contentModelOrderer = contentModelOrderer;
    }

    public RosettaBeanSerializer(RosettaBeanSerializer src, ObjectIdWriter objectIdWriter, Object filterId) {
        super(src, objectIdWriter, filterId);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
    }

    public RosettaBeanSerializer(RosettaBeanSerializer src, Set<String> toIgnore, Set<String> toInclude) {
        super(src, toIgnore, toInclude);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
    }

    protected RosettaBeanSerializer(RosettaBeanSerializer src, BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(src, properties, filteredProperties);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        return new RosettaBeanSerializer(this, objectIdWriter, _propertyFilterId);
    }

    @Override
    public BeanSerializerBase withFilterId(Object filterId) {
        return new RosettaBeanSerializer(this, _objectIdWriter, filterId);
    }

    @Override // since 2.12
    protected BeanSerializerBase withByNameInclusion(Set<String> toIgnore, Set<String> toInclude) {
        return new RosettaBeanSerializer(this, toIgnore, toInclude);
    }

    @Override // since 2.11.1
    protected BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
                                                BeanPropertyWriter[] filteredProperties) {
        return new RosettaBeanSerializer(this, properties, filteredProperties);
    }

    @Override
    public void serialize(Object bean, JsonGenerator g, SerializerProvider provider) throws IOException {
        if (_substitutionMap != null && g instanceof ToXmlGenerator) {
            String substitutedName = _substitutionMap.getSubstitutedName(bean);
            if (substitutedName != null) {
                ((ToXmlGenerator) g).setNextName(new QName(substitutedName));
            }
        }
        if (g instanceof ToXmlGenerator && ((ToXmlGenerator) g).inRoot()) {
            serializeRootElement(bean, (ToXmlGenerator) g, provider);
        } else {
            super.serialize(bean, g, provider);
        }
    }

    public void serializeRootElement(Object bean, ToXmlGenerator g, SerializerProvider provider) throws IOException {
        if (_objectIdWriter != null) {
            // TODO: also include schemaLocation
            _serializeWithObjectId(bean, g, provider, true);
            return;
        }
        g.writeStartObject();
        if (_propertyFilterId != null) {
            // TODO: also include schemaLocation
            serializeFieldsFiltered(bean, g, provider);
        } else {
            serializeFieldsAndAddSchemaLocation(bean, g, provider);
        }
        g.writeEndObject();
    }

    // For non-root beans the base XmlBeanSerializer.serialize() delegates here; reorder child
    // elements to follow the XSD content model when an orderer is configured. schemaLocation is a
    // root-only attribute, so nested beans must NOT emit it (addSchemaLocation = false).
    @Override
    protected void serializeFields(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (_contentModelOrderer != null && gen instanceof ToXmlGenerator) {
            serializeFieldsInContentModelOrder(bean, (ToXmlGenerator) gen, provider, false);
        } else {
            super.serializeFields(bean, gen, provider);
        }
    }

    // Serialize fields as usual, but add the `schemaLocation` attribute at the end of all XML attributes.
    protected void serializeFieldsAndAddSchemaLocation(Object bean, ToXmlGenerator xgen, SerializerProvider provider)
            throws IOException {
        serializeFieldsInContentModelOrder(bean, xgen, provider, true);
    }

    // Shared field-serialisation loop. Child elements are emitted in content-model order; the
    // xsi:schemaLocation attribute is written (after the regular attributes) only when
    // addSchemaLocation is true, which is the case for the root element exclusively.
    private void serializeFieldsInContentModelOrder(Object bean, ToXmlGenerator xgen, SerializerProvider provider,
                                                    boolean addSchemaLocation)
            throws IOException {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }

        final int attrCount = _attributeCount;
        xgen.setNextIsAttribute(true);
        final int textIndex = _textPropertyIndex;
        final QName[] xmlNames = _xmlNames;
        int i = 0;
        final BitSet cdata = _cdata;

        // Permutation of property indices that realises the content-model order. Identity when no
        // orderer is configured or when the order cannot be improved/derived (safe fallback).
        final int[] order = computeContentModelOrder(bean, props, textIndex);

        try {
            if (addSchemaLocation && props.length == 0) {
                writeSchemaLocation(xgen, provider);
            }
            for (final int len = props.length; i < len; ++i) {
                final int pi = order[i];
                // 28-jan-2014, pascal: we don't want to reset the attribute flag if we are an unwrapping serializer
                // that started with nextIsAttribute to true because all properties should be unwrapped as attributes too.
                if (i == attrCount && !isUnwrappingSerializer()) {
                    if (addSchemaLocation) {
                        writeSchemaLocation(xgen, provider);
                    }
                    xgen.setNextIsAttribute(false);
                }
                // also: if this is property to write as text ("unwrap"), need to:
                if (i == textIndex) {
                    xgen.setNextIsUnwrapped(true);
                }
                xgen.setNextName(xmlNames[pi]);
                BeanPropertyWriter prop = props[pi];
                if (prop != null) { // can have nulls in filtered list
                    if ((cdata != null) && cdata.get(pi)) {
                        xgen.setNextIsCData(true);
                        prop.serializeAsField(bean, xgen, provider);
                        xgen.setNextIsCData(false);
                    } else {
                        prop.serializeAsField(bean, xgen, provider);
                    }
                }
                // Reset to avoid next value being written as unwrapped,
                // for example when property is suppressed
                if (i == textIndex) {
                    xgen.setNextIsUnwrapped(false);
                }
            }
            if (_anyGetterWriter != null) {
                // For [#117]: not a clean fix, but with @JsonTypeInfo, we'll end up
                // with accidental attributes otherwise
                xgen.setNextIsAttribute(false);
                _anyGetterWriter.getAndSerialize(bean, xgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[order[i]].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) { // Bit tricky, can't do more calls as stack is full; so:
            JsonMappingException mapE = JsonMappingException.from(xgen,
                    "Infinite recursion (StackOverflowError)");
            String name = (i == props.length) ? "[anySetter]" : props[order[i]].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

    /**
     * Computes a permutation of property indices that emits the content-model properties in the
     * order required by the XSD content model, leaving every other property (and all attributes) in
     * place. Only the slots occupied by <em>present</em> content-model properties are permuted among
     * themselves, so absent properties and non-content-model properties never move. Returns the
     * identity permutation when no orderer is set, when a text/unwrapped property is present (where
     * reordering is unsafe), or when the present set cannot be ordered.
     */
    private int[] computeContentModelOrder(Object bean, BeanPropertyWriter[] props, int textIndex) {
        final int len = props.length;
        final int[] order = new int[len];
        for (int i = 0; i < len; i++) {
            order[i] = i;
        }
        if (_contentModelOrderer == null || textIndex >= 0) {
            return order;
        }
        final Set<String> contentModelProperties = _contentModelOrderer.getContentModelProperties();
        final List<Integer> presentSlots = new ArrayList<>();
        final Map<String, Integer> presentNameToIndex = new LinkedHashMap<>();
        final Set<String> present = new LinkedHashSet<>();
        for (int i = _attributeCount; i < len; i++) {
            final BeanPropertyWriter prop = props[i];
            if (prop == null) {
                continue;
            }
            final String name = prop.getName();
            if (!contentModelProperties.contains(name) || !isPresent(prop, bean)) {
                continue;
            }
            presentSlots.add(i);
            presentNameToIndex.put(name, i);
            present.add(name);
        }
        if (presentSlots.size() <= 1) {
            return order;
        }
        final List<String> ordered = _contentModelOrderer.order(present);
        if (ordered == null || ordered.size() != presentSlots.size()) {
            return order; // fallback: keep default order
        }
        Collections.sort(presentSlots);
        for (int k = 0; k < presentSlots.size(); k++) {
            final Integer target = presentNameToIndex.get(ordered.get(k));
            if (target == null) {
                return identity(len); // inconsistent; bail safely
            }
            order[presentSlots.get(k)] = target;
        }
        return order;
    }

    private static int[] identity(int len) {
        int[] order = new int[len];
        for (int i = 0; i < len; i++) {
            order[i] = i;
        }
        return order;
    }

    private static boolean isPresent(BeanPropertyWriter prop, Object bean) {
        try {
            final Object value = prop.get(bean);
            if (value == null) {
                return false;
            }
            if (value instanceof Collection) {
                return !((Collection<?>) value).isEmpty();
            }
            return true;
        } catch (Exception e) {
            // If we cannot read the value, assume present so it keeps its slot.
            return true;
        }
    }

    private void writeSchemaLocation(ToXmlGenerator xgen, SerializerProvider provider) throws IOException {
        JavaType propType = provider.constructType(String.class);
        PropertyName propertyName = PropertyName.construct(SCHEMA_LOCATION_ATTRIBUTE_PREFIXED_NAME);
        AnnotatedMember member = new VirtualXMLAttribute(_beanType.getRawClass(), SCHEMA_LOCATION_ATTRIBUTE_NAME, propType);
        SimpleBeanPropertyDefinition xmlPropertyDefinition = SimpleBeanPropertyDefinition.construct(provider.getConfig(), member, propertyName, PropertyMetadata.STD_OPTIONAL, JsonInclude.Include.NON_NULL);
        AttributePropertyWriter attrWriter = AttributePropertyWriter.construct(SCHEMA_LOCATION_ATTRIBUTE_NAME, xmlPropertyDefinition, null, _beanType);

        xgen.setNextName(new QName("", attrWriter.getName()));
        try {
            attrWriter.serializeAsField(null, xgen, provider);
        } catch (Exception e) {
            wrapAndThrow(provider, e, null, SCHEMA_LOCATION_ATTRIBUTE_PREFIXED_NAME);
        }
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString() {
        return "RosettaBeanSerializer for " + handledType().getName();
    }
}
