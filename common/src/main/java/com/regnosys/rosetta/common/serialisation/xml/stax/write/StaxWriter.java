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
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.serialization.XMLContentModelOrderer;
import com.regnosys.rosetta.common.serialisation.xml.stax.convert.StaxScalarConverter;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.AttributeBinding;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.RuneTypeIntrospector;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.TypeBinding;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneDataType;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    /**
     * One orderer per type, built on first encounter. An empty {@link Optional} records "this type
     * has no content model", so ordering is only ever derived once per type per writer.
     *
     * <p>Concurrent because a writer is owned by one mapper instance, which may be shared across
     * threads (the {@code ObjectMapper} facade's contract promises thread safety). Both cached
     * values are immutable, so a race merely recomputes an entry.
     */
    private final ConcurrentMap<Class<?>, Optional<XMLContentModelOrderer>> orderers =
            new ConcurrentHashMap<Class<?>, Optional<XMLContentModelOrderer>>();

    /** Getters re-resolved against concrete impl classes; see {@link #resolveGetter}. */
    private final ConcurrentMap<GetterKey, Method> getters = new ConcurrentHashMap<GetterKey, Method>();

    /** Per-type ELEMENT/VIRTUAL attribute lists; see {@link #childAttributesOf}. */
    private final ConcurrentMap<Class<?>, List<AttributeBinding>> childAttributes =
            new ConcurrentHashMap<Class<?>, List<AttributeBinding>>();

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

        Class<?> rootType = resolveRuneType(root);
        if (isScalarType(rootType)) {
            writeScalarRoot(root, rootType, xmlWriter);
        } else {
            TypeBinding binding = introspector.introspect(rootType, config);
            String elementName = binding.getXmlElementName();

            boolean[] hasChildElement = new boolean[MAX_DEPTH];
            Map<String, String> prefixToNs = new HashMap<String, String>();

            writeObject(root, elementName, xmlWriter, 0, prettyPrint, prefixToNs, true, extraRootAttrs, hasChildElement);
        }

        if (prettyPrint) {
            xmlWriter.writeCharacters("\n");
        }

        xmlWriter.flush();
        xmlWriter.close();
        return sw.toString();
    }

    /**
     * A root value with no {@code @RuneAttribute}-annotated type (e.g. a bare {@code ZonedDateTime})
     * has no config binding to resolve an element name from; mirrors {@code StaxReader#isScalarType}
     * so the same class of value round-trips both ways. The element name is the Java simple name,
     * matching the Jackson-era root-level scalar behaviour this replaces.
     */
    private boolean isScalarType(Class<?> type) {
        return !type.isAnnotationPresent(RuneDataType.class)
                && !type.isAnnotationPresent(RosettaDataType.class);
    }

    private void writeScalarRoot(Object root, Class<?> rootType, XMLStreamWriter xmlWriter) throws Exception {
        xmlWriter.writeStartElement(rootType.getSimpleName());
        xmlWriter.writeCharacters(converter.toXmlString(root));
        xmlWriter.writeEndElement();
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

        // 5. Child-producing bindings (ELEMENT and VIRTUAL), in content-model order where the
        // type has a content model, otherwise in bean declaration order.
        for (AttributeBinding attr : orderChildAttributes(binding, object)) {
            if (attr.getXmlRepresentation() == AttributeXMLRepresentation.VIRTUAL) {
                if (attr.isMulti()) {
                    // A repeated unwrapped group has no wrapper element: each occurrence's children
                    // are written inline back-to-back (issue 7 / criterion 17).
                    Object rawList = invoke(attr, object);
                    if (rawList == null) {
                        continue;
                    }
                    for (Object item : (List<?>) rawList) {
                        writeVirtualOccurrence(item, writer, depth, prettyPrint, prefixToNs, hasChildElement);
                    }
                } else {
                    Object value = invoke(attr, object);
                    writeVirtualOccurrence(value, writer, depth, prettyPrint, prefixToNs, hasChildElement);
                }
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

        // Close element
        if (prettyPrint && hasChildElement[depth]) {
            writer.writeCharacters("\n" + indent(depth));
        }
        writer.writeEndElement();
    }

    /**
     * Returns the type's child-producing attributes (ELEMENT and VIRTUAL) in the order they should
     * be written.
     *
     * <p>Without a content model this is plain bean declaration order, which is what the config's
     * design constraint mandates for the (vast majority of) types that carry none. When the type
     * does carry one, the properties the model mentions <em>and</em> that are actually populated on
     * this instance are permuted into model order among the slots they already occupy; every other
     * property — in particular one absent from the content model, such as
     * {@code FpmlFxTargetKnockoutForward.barrier} — keeps its position. This is the same
     * permute-in-place contract the Jackson-era {@code RosettaBeanSerializer} applied, so output
     * ordering is unchanged from that engine.
     *
     * <p>Ordering is at Rosetta-property granularity: a VIRTUAL group's leaves form one contiguous
     * block, so a group the model places before a direct element is emitted before it rather than
     * being flushed last. Leaf order <em>within</em> a group follows the group type's own
     * declaration order, as before.
     */
    private List<AttributeBinding> orderChildAttributes(TypeBinding binding, Object object) throws Exception {
        List<AttributeBinding> children = childAttributesOf(binding);

        XMLContentModelOrderer orderer = ordererFor(binding);
        if (orderer == null) {
            // The overwhelmingly common case: hand back the cached list, allocating nothing.
            return children;
        }

        Set<String> contentModelProperties = orderer.getContentModelProperties();
        List<Integer> presentSlots = new ArrayList<Integer>();
        Map<String, Integer> presentNameToSlot = new LinkedHashMap<String, Integer>();
        Set<String> present = new LinkedHashSet<String>();
        for (int i = 0; i < children.size(); i++) {
            AttributeBinding attr = children.get(i);
            String name = attr.getLogicalName();
            if (!contentModelProperties.contains(name) || !isPopulated(attr, object)) {
                continue;
            }
            presentSlots.add(i);
            presentNameToSlot.put(name, i);
            present.add(name);
        }
        if (presentSlots.size() <= 1) {
            return children;
        }

        List<String> ordered = orderer.order(present);
        if (ordered == null || ordered.size() != presentSlots.size()) {
            // The present combination cannot be consumed by the model; keep declaration order.
            return children;
        }

        List<AttributeBinding> result = new ArrayList<AttributeBinding>(children);
        for (int k = 0; k < presentSlots.size(); k++) {
            Integer source = presentNameToSlot.get(ordered.get(k));
            if (source == null) {
                return children;
            }
            result.set(presentSlots.get(k), children.get(source));
        }
        return result;
    }

    /**
     * The type's child-producing attributes (ELEMENT and VIRTUAL) in declaration order, cached
     * per type. Filtering this on every element written showed up as measurable allocation on
     * deeply nested documents.
     */
    private List<AttributeBinding> childAttributesOf(TypeBinding binding) {
        Class<?> type = binding.getType();
        List<AttributeBinding> cached = childAttributes.get(type);
        if (cached != null) {
            return cached;
        }
        List<AttributeBinding> children = new ArrayList<AttributeBinding>();
        for (AttributeBinding attr : binding.getAttributes()) {
            AttributeXMLRepresentation representation = attr.getXmlRepresentation();
            if (representation == AttributeXMLRepresentation.ELEMENT
                    || representation == AttributeXMLRepresentation.VIRTUAL) {
                children.add(attr);
            }
        }
        children = Collections.unmodifiableList(children);
        List<AttributeBinding> raced = childAttributes.putIfAbsent(type, children);
        return raced != null ? raced : children;
    }

    /**
     * Returns the cached orderer for the type, or {@code null} when it has no content model.
     * An empty {@link Optional} is cached for the latter so a type is only inspected once.
     */
    private XMLContentModelOrderer ordererFor(TypeBinding binding) {
        Class<?> type = binding.getType();
        Optional<XMLContentModelOrderer> cached = orderers.get(type);
        if (cached != null) {
            return cached.orElse(null);
        }
        Optional<XMLContentModel> contentModel = binding.getContentModel();
        Optional<XMLContentModelOrderer> orderer = contentModel.isPresent()
                ? Optional.of(new XMLContentModelOrderer(contentModel.get()))
                : Optional.<XMLContentModelOrderer>empty();
        orderers.putIfAbsent(type, orderer);
        return orderer.orElse(null);
    }

    /** True when the attribute has a value worth emitting (a non-null value, or a non-empty list). */
    private boolean isPopulated(AttributeBinding attr, Object object) throws Exception {
        Object value = invoke(attr, object);
        if (value == null) {
            return false;
        }
        if (attr.isMulti()) {
            return !((List<?>) value).isEmpty();
        }
        return true;
    }

    /**
     * Writes one occurrence of a VIRTUAL attribute's value (a single group instance) inline into
     * the parent element. A no-op when {@code value} is {@code null} or not a Rune model object.
     */
    private void writeVirtualOccurrence(
            Object value,
            XMLStreamWriter writer,
            int depth,
            boolean prettyPrint,
            Map<String, String> prefixToNs,
            boolean[] hasChildElement) throws Exception {
        if (value == null || !(value instanceof RosettaModelObject)) {
            return;
        }
        TypeBinding virtualBinding = introspector.introspect(((RosettaModelObject) value).getType(), config);
        writeChildAttributes(value, virtualBinding, writer, depth, prettyPrint, prefixToNs, hasChildElement);
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
     * The actual serialised object is usually an immutable impl instead, so the stored
     * {@link Method} cannot be invoked on it directly; the equivalent method is looked up by name
     * and signature on the actual object's class, which dispatches correctly.
     *
     * <p>That lookup is cached per (getter, concrete class). Resolving it by catching
     * {@link IllegalArgumentException} instead would mean building an exception and re-running
     * {@link Class#getMethod} for every property of every element written — the dominant cost of
     * serialising a large document.
     */
    private Object invoke(AttributeBinding attr, Object object) throws Exception {
        return resolveGetter(attr.getGetter(), object.getClass()).invoke(object);
    }

    private Method resolveGetter(Method getter, Class<?> concreteClass) throws Exception {
        if (getter.getDeclaringClass().isAssignableFrom(concreteClass)) {
            return getter;
        }
        GetterKey key = new GetterKey(getter, concreteClass);
        Method resolved = getters.get(key);
        if (resolved == null) {
            resolved = concreteClass.getMethod(getter.getName(), getter.getParameterTypes());
            getters.putIfAbsent(key, resolved);
        }
        return resolved;
    }

    /** Cache key for a getter re-resolved against a concrete (usually immutable) impl class. */
    private static final class GetterKey {
        private final Method getter;
        private final Class<?> concreteClass;

        GetterKey(Method getter, Class<?> concreteClass) {
            this.getter = getter;
            this.concreteClass = concreteClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GetterKey)) {
                return false;
            }
            GetterKey other = (GetterKey) o;
            return getter.equals(other.getter) && concreteClass.equals(other.concreteClass);
        }

        @Override
        public int hashCode() {
            return 31 * getter.hashCode() + concreteClass.hashCode();
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
