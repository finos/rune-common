package com.regnosys.rosetta.common.serialisation.xml.stax.read;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLRepresentation;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.stax.convert.StaxScalarConverter;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.AttributeBinding;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.RuneTypeIntrospector;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.TypeBinding;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneDataType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deserialises XML into Rune model objects using a StAX {@link XMLStreamReader}.
 *
 * <p>Handles ELEMENT, ATTRIBUTE, VALUE, and VIRTUAL representations. Distinguishes
 * XML attributes from XML elements natively at the StAX token level (criterion 13 fix):
 * XML attributes are read via {@link XMLStreamReader#getAttributeValue} from the
 * START_ELEMENT token; child elements are handled as START_ELEMENT events in the
 * child-event loop. The two paths are completely separate, so a field named "id" as
 * an ATTRIBUTE and a field named "id" as an ELEMENT never collide.
 *
 * <p>Virtual types are transparent: their children are read from the parent element
 * with no wrapper element (mirror of the writer's {@code writeChildAttributes}).
 *
 * <p>Root-element type inference: when the root element name matches a type in the
 * config, that type is used in preference to the caller-supplied hint type (provided
 * it is a subtype of the hint). This supports top-level substitution where the
 * concrete element name differs from the declared Java type.
 *
 * <p>Post-deserialisation pruning: {@code toBuilder().prune().build()} is applied
 * to the final root object, matching the Jackson path behaviour.
 */
public class StaxReader {

    private final RosettaXMLConfiguration config;
    private final RuneTypeIntrospector introspector;
    private final StaxScalarConverter converter;
    private final ClassLoader classLoader;

    public StaxReader(RosettaXMLConfiguration config, ClassLoader classLoader) {
        this.config = config;
        this.introspector = new RuneTypeIntrospector();
        this.converter = new StaxScalarConverter(config);
        this.classLoader = classLoader;
    }

    /**
     * Reads an XML string into an instance of {@code hintType} (or a subtype inferred
     * from the root element name). Applies post-deserialisation pruning.
     *
     * @param xml      the XML string to read (may contain a leading comment / PI)
     * @param hintType the declared Java type; the actual root element may be a subtype
     * @param <T>      the expected return type
     * @return the deserialised (and pruned) object, or {@code null} if the XML is empty
     * @throws Exception on parse errors or reflection failures
     */
    @SuppressWarnings("unchecked")
    public <T> T read(String xml, Class<T> hintType) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
        try {
            // Skip to the first START_ELEMENT (past any leading comment/PI/DTD)
            while (reader.hasNext() && reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
                reader.next();
            }
            if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
                return null;
            }

            String rootLocalName = reader.getLocalName();
            Class<?> concreteType = inferTypeFromRootElement(rootLocalName, hintType);

            // Scalar types at root level (e.g. ZonedDateTime) are read as text content
            if (isScalarType(concreteType)) {
                String text = reader.getElementText();
                return (T) converter.fromXmlString(text, concreteType);
            }

            Object result = readObject(reader, concreteType);
            return (T) pruneObject(result);
        } finally {
            reader.close();
        }
    }

    // -------------------------------------------------------------------------
    // Root type inference
    // -------------------------------------------------------------------------

    /**
     * Searches the XML config for a type whose {@code xmlElementName} matches the given
     * root element local name. Returns the matching type if it is a subtype of
     * {@code hintType}; otherwise returns {@code hintType} unchanged.
     */
    private Class<?> inferTypeFromRootElement(String elementName, Class<?> hintType) {
        for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> entry : config.getTypeConfigMap().entrySet()) {
            TypeXMLConfiguration typeConfig = entry.getValue();
            String xmlElementName = typeConfig.getXmlElementName().orElse(null);
            if (elementName.equals(xmlElementName)) {
                try {
                    Class<?> candidate = classLoader.loadClass(
                            entry.getKey().getQualifiedName().toString());
                    if (hintType == null || hintType.isAssignableFrom(candidate)) {
                        return candidate;
                    }
                } catch (ClassNotFoundException ignored) {
                    // type not on classpath — skip
                }
            }
        }
        return hintType;
    }

    /**
     * Returns {@code true} when the type is NOT a Rune model type (i.e. scalar, date, etc.).
     * Scalar types are read directly via the converter rather than via the introspector.
     */
    private boolean isScalarType(Class<?> type) {
        return !type.isAnnotationPresent(RuneDataType.class)
                && !type.isAnnotationPresent(RosettaDataType.class);
    }

    // -------------------------------------------------------------------------
    // Core reader loop
    // -------------------------------------------------------------------------

    /**
     * Reads one Rune model object starting at the current START_ELEMENT.
     *
     * <p>Contract: the reader is positioned on the START_ELEMENT for this object's element
     * when the method is called; on return the reader is positioned on the matching
     * END_ELEMENT. This allows the caller to call {@code reader.next()} immediately to
     * advance past the END_ELEMENT.
     */
    private Object readObject(XMLStreamReader reader, Class<?> type) throws Exception {
        TypeBinding binding = introspector.introspect(type, config);
        Object builder = binding.getBuilderType().getDeclaredConstructor().newInstance();

        // 1. Read XML attributes from the current START_ELEMENT.
        //    StAX exposes XML attributes and namespace declarations separately when
        //    IS_NAMESPACE_AWARE is true. getAttributeCount() counts only non-namespace
        //    attributes, so xmlns declarations are naturally excluded.
        readXmlAttributesInto(reader, binding, builder);

        // 2. Lazy virtual builders: created on demand as child elements arrive.
        Map<AttributeBinding, Object> virtualBuilders =
                new LinkedHashMap<AttributeBinding, Object>();

        // 3. VALUE text content accumulator (for types with a VALUE-representation attr)
        StringBuilder textContent = new StringBuilder();

        // 4. Process child events until the END_ELEMENT for this element.
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                String childLocalName = reader.getLocalName();
                handleChildElement(childLocalName, reader, binding, builder, virtualBuilders);
                // After handleChildElement, reader is positioned on the END_ELEMENT of child.
            } else if (event == XMLStreamConstants.CHARACTERS
                    || event == XMLStreamConstants.CDATA) {
                textContent.append(reader.getText());
            }
        }

        // 5. Apply accumulated VALUE text content
        String text = textContent.toString().trim();
        if (!text.isEmpty()) {
            applyValueContent(text, binding, builder);
        }

        // 6. Apply virtual builders: build each virtual object and set it on the parent
        for (Map.Entry<AttributeBinding, Object> entry : virtualBuilders.entrySet()) {
            AttributeBinding attr = entry.getKey();
            Object virtualBuilder = entry.getValue();
            Object builtVirtual = ((RosettaModelObjectBuilder) virtualBuilder).build();
            setOnBuilder(builder, attr, builtVirtual);
        }

        // 7. Build the immutable object
        return ((RosettaModelObjectBuilder) builder).build();
    }

    // -------------------------------------------------------------------------
    // XML attribute handling
    // -------------------------------------------------------------------------

    /**
     * Reads all XML attributes from the current START_ELEMENT token and routes them
     * to the matching ATTRIBUTE-representation attribute bindings on {@code binding}
     * or (one level deep) on VIRTUAL types.
     */
    private void readXmlAttributesInto(
            XMLStreamReader reader,
            TypeBinding binding,
            Object builder) throws Exception {
        int attrCount = reader.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrLocalName = reader.getAttributeLocalName(i);
            String attrValue = reader.getAttributeValue(i);
            applyXmlAttribute(attrLocalName, attrValue, binding, builder, null);
        }
    }

    /**
     * Routes a single XML attribute value to the right binding.
     *
     * <p>Searches: (1) direct ATTRIBUTE-representation bindings on {@code binding};
     * (2) one level into VIRTUAL types' ATTRIBUTE bindings. Unknown attributes are
     * silently ignored (e.g. {@code xsi:schemaLocation} has no binding).
     *
     * @param virtualBuilders accumulates virtual builders; may be {@code null} when this
     *                        call is already processing a virtual type's attributes
     */
    private void applyXmlAttribute(
            String attrLocalName,
            String attrValue,
            TypeBinding binding,
            Object builder,
            Map<AttributeBinding, Object> virtualBuilders) throws Exception {

        // Direct ATTRIBUTE bindings
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ATTRIBUTE) {
                continue;
            }
            if (attr.getXmlName().equals(attrLocalName)) {
                Object value = converter.fromXmlString(attrValue, attr.getValueType());
                setOnBuilder(builder, attr, value);
                return;
            }
        }

        // One level into VIRTUAL types
        if (virtualBuilders != null) {
            for (AttributeBinding virtualAttr : binding.getAttributes()) {
                if (virtualAttr.getXmlRepresentation() != AttributeXMLRepresentation.VIRTUAL) {
                    continue;
                }
                TypeBinding virtualBinding = introspector.introspect(
                        virtualAttr.getValueType(), config);
                for (AttributeBinding childAttr : virtualBinding.getAttributes()) {
                    if (childAttr.getXmlRepresentation() != AttributeXMLRepresentation.ATTRIBUTE) {
                        continue;
                    }
                    if (childAttr.getXmlName().equals(attrLocalName)) {
                        Object vBuilder = getOrCreateVirtualBuilder(virtualAttr, virtualBuilders);
                        Object value = converter.fromXmlString(attrValue, childAttr.getValueType());
                        setOnBuilder(vBuilder, childAttr, value);
                        return;
                    }
                }
            }
        }
        // Unknown XML attribute — ignore (xmlns, xsi:schemaLocation, etc.)
    }

    // -------------------------------------------------------------------------
    // Child element handling
    // -------------------------------------------------------------------------

    /**
     * Handles a child START_ELEMENT by routing it to the right attribute binding on
     * {@code binding} (direct ELEMENT) or one level into a VIRTUAL type's bindings.
     *
     * <p>On return the reader is positioned at the END_ELEMENT of the child element.
     */
    private void handleChildElement(
            String childLocalName,
            XMLStreamReader reader,
            TypeBinding binding,
            Object builder,
            Map<AttributeBinding, Object> virtualBuilders) throws Exception {

        // 1. Direct ELEMENT bindings
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ELEMENT) {
                continue;
            }
            if (attr.getXmlName().equals(childLocalName)) {
                applyChildElement(attr, reader, builder);
                return;
            }
        }

        // 2. One level into VIRTUAL types
        for (AttributeBinding virtualAttr : binding.getAttributes()) {
            if (virtualAttr.getXmlRepresentation() != AttributeXMLRepresentation.VIRTUAL) {
                continue;
            }
            TypeBinding virtualBinding = introspector.introspect(
                    virtualAttr.getValueType(), config);
            AttributeBinding matchedAttr = findDirectElementAttr(childLocalName, virtualBinding);
            if (matchedAttr != null) {
                Object vBuilder = getOrCreateVirtualBuilder(virtualAttr, virtualBuilders);
                applyChildElement(matchedAttr, reader, vBuilder);
                return;
            }
        }

        // 3. Unknown child element — skip over it entirely
        skipElement(reader);
    }

    /**
     * Finds an ELEMENT-representation binding by XML name on {@code binding}'s
     * direct (non-virtual) attributes only.
     */
    private AttributeBinding findDirectElementAttr(String xmlName, TypeBinding binding) {
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() == AttributeXMLRepresentation.ELEMENT
                    && attr.getXmlName().equals(xmlName)) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Applies a matched child element's content to the target builder.
     *
     * <p>For nested Rune objects, recurses via {@link #readObject}. For scalars, reads
     * the text content via {@link XMLStreamReader#getElementText()}.
     *
     * <p>On return the reader is positioned at the END_ELEMENT of the child element.
     * This is guaranteed both when calling {@code readObject} (its contract) and when
     * calling {@code getElementText()} (per XMLStreamReader Javadoc).
     */
    private void applyChildElement(
            AttributeBinding attr,
            XMLStreamReader reader,
            Object targetBuilder) throws Exception {
        if (attr.isRosettaModelObject()) {
            Object childObj = readObject(reader, attr.getValueType());
            setOnBuilder(targetBuilder, attr, childObj);
        } else {
            // getElementText() reads text content and leaves reader on END_ELEMENT
            String text = reader.getElementText();
            Object value = converter.fromXmlString(text, attr.getValueType());
            setOnBuilder(targetBuilder, attr, value);
        }
    }

    /**
     * Sets (or accumulates) the VALUE-representation text content on the builder.
     */
    private void applyValueContent(
            String text, TypeBinding binding, Object builder) throws Exception {
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() == AttributeXMLRepresentation.VALUE) {
                Object value = converter.fromXmlString(text, attr.getValueType());
                setOnBuilder(builder, attr, value);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Builder helpers
    // -------------------------------------------------------------------------

    /**
     * Sets or adds a value on the builder via reflection.
     * Uses the adder for multi-cardinality attributes, the setter for single.
     */
    private void setOnBuilder(Object builder, AttributeBinding attr, Object value)
            throws Exception {
        if (value == null) {
            return;
        }
        if (attr.isMulti()) {
            if (attr.getAdder() != null) {
                attr.getAdder().invoke(builder, value);
            }
        } else {
            if (attr.getSetter() != null) {
                attr.getSetter().invoke(builder, value);
            }
        }
    }

    /**
     * Returns the virtual builder for {@code virtualAttr}, creating one if absent.
     * The builder is cached in {@code virtualBuilders} by the attribute binding instance.
     */
    private Object getOrCreateVirtualBuilder(
            AttributeBinding virtualAttr,
            Map<AttributeBinding, Object> virtualBuilders) throws Exception {
        Object vBuilder = virtualBuilders.get(virtualAttr);
        if (vBuilder == null) {
            TypeBinding virtualBinding = introspector.introspect(
                    virtualAttr.getValueType(), config);
            vBuilder = virtualBinding.getBuilderType().getDeclaredConstructor().newInstance();
            virtualBuilders.put(virtualAttr, vBuilder);
        }
        return vBuilder;
    }

    // -------------------------------------------------------------------------
    // Skip and prune utilities
    // -------------------------------------------------------------------------

    /**
     * Skips past an element the reader is currently positioned on (START_ELEMENT).
     * Handles nested elements. On return the reader is positioned at the END_ELEMENT
     * of the skipped element.
     */
    private void skipElement(XMLStreamReader reader) throws Exception {
        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }

    /**
     * Applies the standard Rune post-deserialisation prune:
     * {@code toBuilder().prune().build()}.
     */
    private Object pruneObject(Object value) {
        if (value instanceof RosettaModelObjectBuilder) {
            return ((RosettaModelObjectBuilder) value).prune();
        }
        if (value instanceof RosettaModelObject) {
            return ((RosettaModelObject) value).toBuilder().prune().build();
        }
        return value;
    }
}
