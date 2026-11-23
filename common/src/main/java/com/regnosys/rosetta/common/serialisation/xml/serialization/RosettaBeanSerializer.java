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
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap.XMLFullyQualifiedName;
import com.regnosys.rosetta.common.serialisation.xml.VirtualXMLAttribute;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.RosettaModelObject;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 *
 * <p>Also writes an {@code xsi:type} attribute on an element whose value is a strict subtype of the type
 * the element is declared with. It names the XSD type, from {@code xmlTypeFullyQualifiedName} in the XML
 * configuration, of the value's type, or else of its nearest supertype that is still a strict subtype of
 * the declared type and has one. When none has one, no {@code xsi:type} is written. The declared type
 * comes from the property this serializer is contextualized for.
 */
public class RosettaBeanSerializer extends XmlBeanSerializer {
    public static final String SCHEMA_LOCATION_ATTRIBUTE_NAME = "schemaLocation";
    private static final String SCHEMA_LOCATION_ATTRIBUTE_PREFIXED_NAME = "xsi:" + SCHEMA_LOCATION_ATTRIBUTE_NAME;
    /** Provider attribute carrying the xsi:type for the element being started, from serialize() to serializeFields(). */
    private static final Object PENDING_XSI_TYPE = new Object();
    /** Provider attribute: prefix to namespace, for the xmlns attributes the elements being written declare literally. */
    private static final Object LITERAL_NAMESPACES = new Object();

    private final SubstitutionMap _substitutionMap;

    // When present, child elements are serialised in the order dictated by the XSD content model
    // (computed per object against the present data), rather than the default property order.
    private final XMLContentModelOrderer _contentModelOrderer;

    // Where the XSD type of a subtype is looked up, to write it as xsi:type. Null when not configured.
    private final RosettaXMLConfiguration _xmlConfiguration;

    // The type of the property this serializer writes, when it has been contextualized for one.
    private final Class<?> _declaredType;

    // Prefix to namespace, for the `xmlns` and `xmlns:<prefix>` attributes this type's XML configuration
    // writes. They are plain attributes to the XML writer, which therefore does not know these prefixes
    // are bound.
    private final Map<String, String> _literalNamespaces;

    public RosettaBeanSerializer(XmlBeanSerializer src, SubstitutionMap substitutionMap) {
        this(src, substitutionMap, null);
    }

    public RosettaBeanSerializer(XmlBeanSerializer src, SubstitutionMap substitutionMap,
                                 XMLContentModelOrderer contentModelOrderer) {
        this(src, substitutionMap, contentModelOrderer, null, Collections.emptyMap());
    }

    public RosettaBeanSerializer(XmlBeanSerializer src, SubstitutionMap substitutionMap,
                                 XMLContentModelOrderer contentModelOrderer, RosettaXMLConfiguration xmlConfiguration,
                                 Map<String, String> literalNamespaces) {
        super(src);
        this._substitutionMap = substitutionMap;
        this._contentModelOrderer = contentModelOrderer;
        this._xmlConfiguration = xmlConfiguration;
        this._declaredType = null;
        this._literalNamespaces = literalNamespaces;
    }

    private RosettaBeanSerializer(RosettaBeanSerializer src, Class<?> declaredType) {
        super(src);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
        this._xmlConfiguration = src._xmlConfiguration;
        this._declaredType = declaredType;
        this._literalNamespaces = src._literalNamespaces;
    }

    public RosettaBeanSerializer(RosettaBeanSerializer src, ObjectIdWriter objectIdWriter, Object filterId) {
        super(src, objectIdWriter, filterId);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
        this._xmlConfiguration = src._xmlConfiguration;
        this._declaredType = src._declaredType;
        this._literalNamespaces = src._literalNamespaces;
    }

    public RosettaBeanSerializer(RosettaBeanSerializer src, Set<String> toIgnore, Set<String> toInclude) {
        super(src, toIgnore, toInclude);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
        this._xmlConfiguration = src._xmlConfiguration;
        this._declaredType = src._declaredType;
        this._literalNamespaces = src._literalNamespaces;
    }

    protected RosettaBeanSerializer(RosettaBeanSerializer src, BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(src, properties, filteredProperties);
        this._substitutionMap = src._substitutionMap;
        this._contentModelOrderer = src._contentModelOrderer;
        this._xmlConfiguration = src._xmlConfiguration;
        this._declaredType = src._declaredType;
        this._literalNamespaces = src._literalNamespaces;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) throws JsonMappingException {
        JsonSerializer<?> contextual = super.createContextual(provider, property);
        Class<?> declaredType = declaredTypeOf(property);
        if (contextual instanceof RosettaBeanSerializer
                && !Objects.equals(declaredType, ((RosettaBeanSerializer) contextual)._declaredType)) {
            return new RosettaBeanSerializer((RosettaBeanSerializer) contextual, declaredType);
        }
        return contextual;
    }

    // A property written through a substitution group names the subtype by its element name, so it
    // needs no xsi:type.
    private static Class<?> declaredTypeOf(BeanProperty property) {
        if (property == null || property instanceof SubstitutingBeanPropertyWriter) {
            return null;
        }
        JavaType type = property.getType();
        if (type.isContainerType() && type.getContentType() != null) {
            type = type.getContentType();
        }
        return type.getRawClass();
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
        Object enclosingLiteralNamespaces = provider.getAttribute(LITERAL_NAMESPACES);
        if (!_literalNamespaces.isEmpty()) {
            Map<String, String> inScope = new LinkedHashMap<>();
            if (enclosingLiteralNamespaces != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> enclosing = (Map<String, String>) enclosingLiteralNamespaces;
                inScope.putAll(enclosing);
            }
            inScope.putAll(_literalNamespaces);
            provider.setAttribute(LITERAL_NAMESPACES, inScope);
        }
        try {
            if (g instanceof ToXmlGenerator && ((ToXmlGenerator) g).inRoot()) {
                serializeRootElement(bean, (ToXmlGenerator) g, provider);
            } else {
                XMLFullyQualifiedName xsiType = g instanceof ToXmlGenerator ? xsiTypeOf(bean) : null;
                if (xsiType == null) {
                    super.serialize(bean, g, provider);
                } else {
                    // Consumed by serializeFields once the start tag is written, before any child bean is.
                    provider.setAttribute(PENDING_XSI_TYPE, xsiType);
                    try {
                        super.serialize(bean, g, provider);
                    } finally {
                        provider.setAttribute(PENDING_XSI_TYPE, null);
                    }
                }
            }
        } finally {
            provider.setAttribute(LITERAL_NAMESPACES, enclosingLiteralNamespaces);
        }
    }

    private XMLFullyQualifiedName xsiTypeOf(Object bean) {
        if (_declaredType == null || _xmlConfiguration == null || !(bean instanceof RosettaModelObject)) {
            return null;
        }
        Class<?> type = ((RosettaModelObject) bean).getType();
        if (!_declaredType.isAssignableFrom(type)) {
            return null;
        }
        // A type with no XSD type of its own is written as its nearest supertype that has one, so that it
        // reads back as that type rather than as the declared one.
        for (Class<?> candidate = type; candidate != null && !candidate.equals(_declaredType);
             candidate = findExtendedRuneTypeWithinDeclaredType(candidate)) {
            Optional<String> xsdTypeName = _xmlConfiguration.getConfigurationForType(ModelSymbolId.fromQualifiedName(candidate.getName()))
                    .flatMap(TypeXMLConfiguration::getXmlTypeFullyQualifiedName);
            if (xsdTypeName.isPresent()) {
                return new XMLFullyQualifiedName(xsdTypeName.get());
            }
        }
        return null;
    }

    // The Rune type the given one extends, if that is the declared type or a subtype of it; otherwise null.
    private Class<?> findExtendedRuneTypeWithinDeclaredType(Class<?> type) {
        for (Class<?> supertype : type.getInterfaces()) {
            if (_declaredType.isAssignableFrom(supertype)) {
                return supertype;
            }
        }
        return null;
    }

    // Called with the element's start tag written and no attributes yet, so namespace declarations can
    // still be added to it.
    private void writePendingXsiType(JsonGenerator gen, SerializerProvider provider) throws IOException {
        Object pending = provider.getAttribute(PENDING_XSI_TYPE);
        if (pending == null || !(gen instanceof ToXmlGenerator)) {
            return;
        }
        provider.setAttribute(PENDING_XSI_TYPE, null);
        XMLFullyQualifiedName xsiType = (XMLFullyQualifiedName) pending;
        XMLStreamWriter writer = ((ToXmlGenerator) gen).getStaxWriter();
        @SuppressWarnings("unchecked")
        Map<String, String> literalNamespaces = (Map<String, String>) provider.getAttribute(LITERAL_NAMESPACES);
        Map<String, String> literal = literalNamespaces == null ? Collections.emptyMap() : literalNamespaces;
        try {
            String typeNamespace = xsiType.getNamespace();
            if (typeNamespace == null) {
                // An unprefixed QName is in the default namespace, so a type in no namespace cannot be named here.
                String defaultNamespace = findDefaultNamespaceInScope(writer, literal);
                if (defaultNamespace != null) {
                    throw new IOException("Cannot write xsi:type " + xsiType.getName() + ": its XSD type is in no namespace, "
                            + "but the default namespace here is " + defaultNamespace + ", so it would read back as "
                            + "{" + defaultNamespace + "}" + xsiType.getName() + ". Give xmlTypeFullyQualifiedName as namespace/localName.");
                }
            }
            String typePrefix = typeNamespace == null ? "" : prefixFor(writer, literal, typeNamespace, "ns");
            String value = typePrefix.isEmpty() ? xsiType.getName() : typePrefix + ":" + xsiType.getName();
            String literalXsiPrefix = literalPrefixFor(writer, literal, XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            if (literalXsiPrefix != null && !literalXsiPrefix.isEmpty()) {
                // Written by name, like xsi:schemaLocation: the writer does not know the prefix is bound, and
                // would otherwise declare it again on this element.
                writer.writeAttribute(literalXsiPrefix + ":type", value);
            } else {
                String xsiPrefix = prefixFor(writer, Collections.emptyMap(), XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
                writer.writeAttribute(xsiPrefix, XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", value);
            }
        } catch (XMLStreamException e) {
            throw new IOException("Could not write xsi:type " + xsiType, e);
        }
    }

    // The prefix bound to the namespace here, declaring one on the current element if there is none.
    // A literal declaration on an enclosing element counts unless the writer has bound its prefix since.
    private static String prefixFor(XMLStreamWriter writer, Map<String, String> literalNamespaces,
                                    String namespace, String preferredPrefix) throws XMLStreamException {
        String prefix = writer.getPrefix(namespace);
        if (prefix != null && !(prefix.isEmpty() && XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(namespace))) {
            return prefix;
        }
        String literalPrefix = literalPrefixFor(writer, literalNamespaces, namespace);
        if (literalPrefix != null) {
            return literalPrefix;
        }
        NamespaceContext context = writer.getNamespaceContext();
        prefix = preferredPrefix;
        for (int i = 1; isBoundByWriter(context, prefix) || literalNamespaces.containsKey(prefix); i++) {
            prefix = preferredPrefix + i;
        }
        writer.setPrefix(prefix, namespace);
        writer.writeNamespace(prefix, namespace);
        return prefix;
    }

    // The prefix an enclosing element's literal declaration binds to the namespace, unless the writer has
    // bound that prefix to something else since.
    private static String literalPrefixFor(XMLStreamWriter writer, Map<String, String> literalNamespaces, String namespace) {
        NamespaceContext context = writer.getNamespaceContext();
        for (Map.Entry<String, String> literal : literalNamespaces.entrySet()) {
            if (literal.getValue().equals(namespace) && !isBoundByWriter(context, literal.getKey())) {
                return literal.getKey();
            }
        }
        return null;
    }

    // The non-empty default namespace in scope, bound by the writer or by an enclosing literal xmlns; otherwise null.
    private static String findDefaultNamespaceInScope(XMLStreamWriter writer, Map<String, String> literalNamespaces) {
        String bound = writer.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
        String namespace = bound != null && !bound.isEmpty() ? bound : literalNamespaces.get(XMLConstants.DEFAULT_NS_PREFIX);
        return namespace == null || namespace.isEmpty() ? null : namespace;
    }

    private static boolean isBoundByWriter(NamespaceContext context, String prefix) {
        String namespace = context.getNamespaceURI(prefix);
        return namespace != null && !namespace.isEmpty();
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
        writePendingXsiType(gen, provider);
        if (_contentModelOrderer != null && gen instanceof ToXmlGenerator) {
            serializeFieldsInContentModelOrder(bean, (ToXmlGenerator) gen, provider, false);
        } else {
            super.serializeFields(bean, gen, provider);
        }
    }

    @Override
    protected void serializeFieldsFiltered(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
        writePendingXsiType(gen, provider);
        super.serializeFieldsFiltered(bean, gen, provider);
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
            // Since Jackson 2.19 an any-getter is a BeanPropertyWriter inside `props`, so the loop above writes it.
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
