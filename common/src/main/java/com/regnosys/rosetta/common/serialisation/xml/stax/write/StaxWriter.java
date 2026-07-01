package com.regnosys.rosetta.common.serialisation.xml.stax.write;

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
import com.regnosys.rosetta.common.serialisation.xml.stax.convert.StaxScalarConverter;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.AttributeBinding;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.RuneTypeIntrospector;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.TypeBinding;
import com.rosetta.model.lib.RosettaModelObject;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialises a Rune model object to an XML string using a StAX {@link XMLStreamWriter}.
 *
 * <p>Handles ELEMENT, ATTRIBUTE, VALUE, and VIRTUAL representations.
 * VIRTUAL attributes are transparent wrappers whose children are written directly
 * into the parent element (no wrapper element is emitted).
 * No XML declaration is written.
 */
public class StaxWriter {

    private static final int MAX_DEPTH = 50;

    private final RosettaXMLConfiguration config;
    private final RuneTypeIntrospector introspector;
    private final StaxScalarConverter converter;

    public StaxWriter(RosettaXMLConfiguration config) {
        this.config = config;
        this.introspector = new RuneTypeIntrospector();
        this.converter = new StaxScalarConverter(config);
    }

    /**
     * Serialises a Rune model object to an XML string.
     * No XML declaration is included in the output.
     *
     * @param root           the root object to serialise
     * @param prettyPrint    if true, 2-space indentation with newlines
     * @param extraRootAttrs extra attributes written on root element AFTER constant attributes
     *                       (e.g. Map with "xsi:schemaLocation" -&gt; "urn:my.schema ../schema/schema.xsd")
     * @return XML string (no XML declaration)
     * @throws Exception on write error or reflection failure
     */
    public String write(Object root, boolean prettyPrint, Map<String, String> extraRootAttrs) throws Exception {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(sw);

        TypeBinding binding = introspector.introspect(resolveRuneType(root), config);
        String elementName = binding.getXmlElementName();

        boolean[] hasChildElement = new boolean[MAX_DEPTH];
        Map<String, String> prefixToNs = new HashMap<String, String>();

        writeObject(root, elementName, xmlWriter, 0, prettyPrint, prefixToNs, true, extraRootAttrs, hasChildElement);

        if (prettyPrint) {
            xmlWriter.writeCharacters("\n");
        }

        xmlWriter.flush();
        xmlWriter.close();
        return sw.toString();
    }

    /**
     * Resolves the Rune interface type from an object instance.
     * Generated impl classes are inner classes of the interface, so the declaring class
     * IS the annotated interface type (e.g. {@code TopLevel.TopLevelImpl} → {@code TopLevel}).
     * For {@link RosettaModelObject} instances, we use {@code getType()} which always
     * returns the interface class directly.
     */
    private Class<?> resolveRuneType(Object object) {
        if (object instanceof RosettaModelObject) {
            return ((RosettaModelObject) object).getType();
        }
        return object.getClass();
    }

    private void writeObject(
            Object object,
            String elementName,
            XMLStreamWriter writer,
            int depth,
            boolean prettyPrint,
            Map<String, String> prefixToNs,
            boolean isRoot,
            Map<String, String> extraRootAttrs,
            boolean[] hasChildElement) throws Exception {

        // Pretty-print: newline + indent before start element (not at depth 0)
        if (prettyPrint && depth > 0) {
            writer.writeCharacters("\n" + indent(depth));
        }

        writer.writeStartElement(elementName);

        TypeBinding binding = introspector.introspect(resolveRuneType(object), config);

        // 1. Constant attributes (only on root element)
        if (isRoot) {
            for (Map.Entry<String, String> entry : binding.getXmlConstantAttributes().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if ("xmlns".equals(key)) {
                    writer.writeDefaultNamespace(value);
                } else if (key.startsWith("xmlns:")) {
                    String prefix = key.substring("xmlns:".length());
                    writer.writeNamespace(prefix, value);
                    prefixToNs.put(prefix, value);
                } else {
                    writer.writeAttribute(key, value);
                }
            }

            // 2. Extra root attributes (after constant attrs)
            if (extraRootAttrs != null) {
                for (Map.Entry<String, String> entry : extraRootAttrs.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    int colonIdx = key.indexOf(':');
                    if (colonIdx >= 0) {
                        String prefix = key.substring(0, colonIdx);
                        String localName = key.substring(colonIdx + 1);
                        String nsUri = prefixToNs.get(prefix);
                        if (nsUri != null) {
                            writer.writeAttribute(nsUri, localName, value);
                        } else {
                            writer.writeAttribute(key, value);
                        }
                    } else {
                        writer.writeAttribute(key, value);
                    }
                }
            }
        }

        // 3. ATTRIBUTE-representation bindings
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ATTRIBUTE) {
                continue;
            }
            Object value = invoke(attr, object);
            if (value == null) {
                continue;
            }
            String xmlStr = converter.toXmlString(value);
            writer.writeAttribute(attr.getXmlName(), xmlStr);
        }

        // Track whether any child element was written at this depth
        hasChildElement[depth] = false;

        // 4. VALUE-representation bindings (written as text content, not a child element)
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.VALUE) {
                continue;
            }
            Object value = invoke(attr, object);
            if (value == null) {
                continue;
            }
            String xmlStr = converter.toXmlString(value);
            writer.writeCharacters(xmlStr);
        }

        // 5. ELEMENT-representation bindings (child elements)
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ELEMENT) {
                continue;
            }

            if (attr.isMulti()) {
                Object rawList = invoke(attr, object);
                if (rawList == null) {
                    continue;
                }
                List<?> list = (List<?>) rawList;
                if (list.isEmpty()) {
                    continue;
                }
                for (Object item : list) {
                    if (item == null) {
                        continue;
                    }
                    hasChildElement[depth] = true;
                    if (attr.isRosettaModelObject()) {
                        hasChildElement[depth + 1] = false;
                        writeObject(item, resolveElementName(attr, item), writer, depth + 1, prettyPrint,
                                prefixToNs, false, null, hasChildElement);
                    } else {
                        String xmlStr = converter.toXmlString(item);
                        writeLeafElement(attr.getXmlName(), xmlStr, writer, depth, prettyPrint);
                    }
                }
            } else {
                Object value = invoke(attr, object);
                if (value == null) {
                    continue;
                }
                hasChildElement[depth] = true;
                if (attr.isRosettaModelObject()) {
                    hasChildElement[depth + 1] = false;
                    writeObject(value, resolveElementName(attr, value), writer, depth + 1, prettyPrint,
                            prefixToNs, false, null, hasChildElement);
                } else {
                    String xmlStr = converter.toXmlString(value);
                    writeLeafElement(attr.getXmlName(), xmlStr, writer, depth, prettyPrint);
                }
            }
        }

        // 6. VIRTUAL-representation bindings (inline children of the virtual type into the parent)
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.VIRTUAL) {
                continue;
            }
            Object value = invoke(attr, object);
            if (value == null) {
                continue;
            }
            if (value instanceof RosettaModelObject) {
                TypeBinding virtualBinding = introspector.introspect(
                        ((RosettaModelObject) value).getType(), config);
                writeChildAttributes(value, virtualBinding, writer, depth, prettyPrint, prefixToNs, hasChildElement);
            }
        }

        // Close element
        if (prettyPrint && hasChildElement[depth]) {
            writer.writeCharacters("\n" + indent(depth));
        }
        writer.writeEndElement();
    }

    /**
     * Writes the children of a VIRTUAL attribute's value directly into the parent element.
     *
     * <p>A VIRTUAL attribute is a transparent wrapper: instead of writing a child element for
     * the attribute, its own children are written inline into the parent element as if the
     * wrapper type didn't exist.  The {@code depth} and {@code hasChildElement} slot used are
     * those of the PARENT (no new depth level is entered because no start-element is emitted).
     *
     * <p>Handles ELEMENT, ATTRIBUTE, VALUE, and recursively nested VIRTUAL attributes.
     */
    private void writeChildAttributes(
            Object virtualObject,
            TypeBinding virtualBinding,
            XMLStreamWriter writer,
            int depth,
            boolean prettyPrint,
            Map<String, String> prefixToNs,
            boolean[] hasChildElement) throws Exception {

        for (AttributeBinding attr : virtualBinding.getAttributes()) {
            switch (attr.getXmlRepresentation()) {
                case ATTRIBUTE: {
                    Object value = invoke(attr, virtualObject);
                    if (value == null) {
                        break;
                    }
                    String xmlStr = converter.toXmlString(value);
                    writer.writeAttribute(attr.getXmlName(), xmlStr);
                    break;
                }
                case VALUE: {
                    Object value = invoke(attr, virtualObject);
                    if (value == null) {
                        break;
                    }
                    String xmlStr = converter.toXmlString(value);
                    writer.writeCharacters(xmlStr);
                    break;
                }
                case ELEMENT: {
                    if (attr.isMulti()) {
                        Object rawList = invoke(attr, virtualObject);
                        if (rawList == null) {
                            break;
                        }
                        List<?> list = (List<?>) rawList;
                        if (list.isEmpty()) {
                            break;
                        }
                        for (Object item : list) {
                            if (item == null) {
                                continue;
                            }
                            hasChildElement[depth] = true;
                            if (attr.isRosettaModelObject()) {
                                hasChildElement[depth + 1] = false;
                                writeObject(item, resolveElementName(attr, item), writer, depth + 1,
                                        prettyPrint, prefixToNs, false, null, hasChildElement);
                            } else {
                                String xmlStr = converter.toXmlString(item);
                                writeLeafElement(attr.getXmlName(), xmlStr, writer, depth, prettyPrint);
                            }
                        }
                    } else {
                        Object value = invoke(attr, virtualObject);
                        if (value == null) {
                            break;
                        }
                        hasChildElement[depth] = true;
                        if (attr.isRosettaModelObject()) {
                            hasChildElement[depth + 1] = false;
                            writeObject(value, resolveElementName(attr, value), writer, depth + 1,
                                    prettyPrint, prefixToNs, false, null, hasChildElement);
                        } else {
                            String xmlStr = converter.toXmlString(value);
                            writeLeafElement(attr.getXmlName(), xmlStr, writer, depth, prettyPrint);
                        }
                    }
                    break;
                }
                case VIRTUAL: {
                    Object value = invoke(attr, virtualObject);
                    if (value == null) {
                        break;
                    }
                    if (value instanceof RosettaModelObject) {
                        TypeBinding nestedBinding = introspector.introspect(
                                ((RosettaModelObject) value).getType(), config);
                        writeChildAttributes(value, nestedBinding, writer, depth, prettyPrint,
                                prefixToNs, hasChildElement);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * Resolves the XML element name to use when writing a value for an attribute.
     *
     * <p>For substitution-group attributes (those with {@link AttributeBinding#getElementRef()}
     * present), the element name is derived from the concrete type of the value, not from
     * the attribute's own {@link AttributeBinding#getXmlName()}.  For example, if the
     * attribute is {@code animal} (elementRef = "urn:my.schema/animal") and the actual
     * value is a {@code Goat}, the concrete binding resolves to {@code "goat"}.
     *
     * <p>For non-substitution attributes, {@link AttributeBinding#getXmlName()} is returned.
     */
    private String resolveElementName(AttributeBinding attr, Object value) {
        if (attr.getElementRef().isPresent() && value instanceof RosettaModelObject) {
            Class<?> concreteType = ((RosettaModelObject) value).getType();
            TypeBinding concreteBinding = introspector.introspect(concreteType, config);
            return concreteBinding.getXmlElementName();
        }
        return attr.getXmlName();
    }

    /**
     * Writes a leaf scalar element like {@code <Name>text</Name>}.
     * With pretty-print: writes newline+indent before the start tag.
     */
    private void writeLeafElement(
            String xmlName,
            String text,
            XMLStreamWriter writer,
            int parentDepth,
            boolean prettyPrint) throws Exception {
        if (prettyPrint) {
            writer.writeCharacters("\n" + indent(parentDepth + 1));
        }
        writer.writeStartElement(xmlName);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    /**
     * Invokes the getter for an {@link AttributeBinding} on the given object.
     *
     * <p>The getter stored in {@link AttributeBinding} is obtained from the builder impl class.
     * The actual serialised object may be an immutable impl (not the builder).
     * Both implement the same Rune interface, so we look up the method by name and signature
     * on the actual object's class before invoking, which correctly dispatches to the impl.
     */
    private Object invoke(AttributeBinding attr, Object object) throws Exception {
        java.lang.reflect.Method getter = attr.getGetter();
        try {
            // Try the stored getter first (works if object is a builder impl)
            return getter.invoke(object);
        } catch (IllegalArgumentException e) {
            // Object is an immutable impl — look up the same method on its class
            java.lang.reflect.Method resolved = object.getClass().getMethod(
                    getter.getName(), getter.getParameterTypes());
            return resolved.invoke(object);
        }
    }

    private static String indent(int depth) {
        StringBuilder sb = new StringBuilder(depth * 2);
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
}
