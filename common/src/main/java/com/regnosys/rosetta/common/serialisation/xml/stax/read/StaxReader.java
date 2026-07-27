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
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.ContentModelRouter;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 *
 * <p>Substitution-group member elements (attributes whose config carries an
 * {@code elementRef}) are resolved via {@link SubstitutionResolver}, which picks the
 * concrete type by namespace-aware lookup first, falling back to local-name-only lookup
 * (criteria 15/16) — see {@link #resolveElementMatch}.
 *
 * <p>Types whose config carries an ambiguous {@code contentModel} are read through
 * {@link #readRoutedObject}: their children are buffered in document order and routed by
 * {@link ContentModelRouter} before being bound, which is what lets one XML element name reach
 * different Rosetta slots depending on position (criterion 4). Elements are matched against the
 * content model with their real StAX namespace, so a namespace-qualified model never falls back to
 * permissive local-name matching the way the Jackson path was forced to.
 */
public class StaxReader {

    private final RosettaXMLConfiguration config;
    private final RuneTypeIntrospector introspector;
    private final StaxScalarConverter converter;
    private final ClassLoader classLoader;
    private final SubstitutionResolver substitutionResolver;
    private final VirtualPathAssembler virtualPathAssembler;
    private final Map<Class<?>, ContentModelRouter> routerCache = new HashMap<>();

    public StaxReader(RosettaXMLConfiguration config, ClassLoader classLoader) {
        this.config = config;
        this.introspector = new RuneTypeIntrospector();
        this.converter = new StaxScalarConverter(config);
        this.classLoader = classLoader;
        this.substitutionResolver = new SubstitutionResolver(config, classLoader);
        this.virtualPathAssembler = new VirtualPathAssembler(introspector, config, this::readLeafValue);
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

            XmlCursor cursor = new StaxCursor(reader);
            String rootLocalName = cursor.getLocalName();
            Class<?> concreteType = inferTypeFromRootElement(rootLocalName, hintType);

            // Scalar types at root level (e.g. ZonedDateTime) are read as text content
            if (isScalarType(concreteType)) {
                String text = cursor.getElementText();
                return (T) converter.fromXmlString(text, concreteType);
            }

            Object result = readObject(cursor, concreteType);
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
     * <p>Contract: the cursor is positioned on the START_ELEMENT for this object's element
     * when the method is called; on return the cursor is positioned on the matching
     * END_ELEMENT. This allows the caller to call {@code cursor.next()} immediately to
     * advance past the END_ELEMENT.
     */
    private Object readObject(XmlCursor cursor, Class<?> type) throws Exception {
        TypeBinding binding = introspector.introspect(type, config);

        // Ambiguous types need all children in hand before any can be bound — see readRoutedObject.
        ContentModelRouter router = routerFor(type, binding);
        if (router != null) {
            return readRoutedObject(cursor, binding, router);
        }

        Object builder = BuilderAccess.newBuilder(binding);

        // 1. Read XML attributes from the current START_ELEMENT.
        //    StAX exposes XML attributes and namespace declarations separately when
        //    IS_NAMESPACE_AWARE is true. getAttributeCount() counts only non-namespace
        //    attributes, so xmlns declarations are naturally excluded.
        readXmlAttributesInto(cursor, binding, builder);

        // 2. Lazy virtual builders: created on demand as child elements arrive.
        Map<AttributeBinding, VirtualGroupState> virtualBuilders =
                new LinkedHashMap<AttributeBinding, VirtualGroupState>();

        // 3. VALUE text content accumulator (for types with a VALUE-representation attr)
        StringBuilder textContent = new StringBuilder();

        // 4. Process child events until the END_ELEMENT for this element.
        while (cursor.hasNext()) {
            int event = cursor.next();
            if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                String childLocalName = cursor.getLocalName();
                handleChildElement(childLocalName, cursor, binding, builder, virtualBuilders);
                // After handleChildElement, cursor is positioned on the END_ELEMENT of child.
            } else if (event == XMLStreamConstants.CHARACTERS
                    || event == XMLStreamConstants.CDATA) {
                textContent.append(cursor.getText());
            }
        }

        // 5. Apply accumulated VALUE text content
        applyValueContent(textContent, binding, builder);

        // 6. Apply virtual builders: build each virtual object and set it on the parent
        applyVirtualBuilders(virtualBuilders, builder);

        // 7. Build the immutable object
        return ((RosettaModelObjectBuilder) builder).build();
    }

    // -------------------------------------------------------------------------
    // Content-model disambiguation
    // -------------------------------------------------------------------------

    /**
     * Returns the router for {@code type}, or {@code null} when the type needs no content-model
     * routing (no content model, or one that plain name-based binding already handles correctly).
     * Routers are cached per type: building one indexes the whole content model.
     */
    private ContentModelRouter routerFor(Class<?> type, TypeBinding binding) {
        if (routerCache.containsKey(type)) {
            return routerCache.get(type);
        }
        Optional<XMLContentModel> contentModel = binding.getContentModel();
        ContentModelRouter router = null;
        if (contentModel.isPresent() && ContentModelRouter.requiresRouting(contentModel.get())) {
            router = new ContentModelRouter(contentModel.get(), type.getName());
        }
        routerCache.put(type, router);
        return router;
    }

    /**
     * Reads one object whose type carries an ambiguous content model.
     *
     * <p>Every child element is buffered in document order (name + namespace + subtree), then the
     * whole sequence is handed to {@link ContentModelRouter}. Each child is then bound according to
     * its routed path:
     * <ul>
     *   <li>multi-segment path — routed into a virtual (unwrapped) sub-object by
     *       {@link VirtualPathAssembler}, grouped by content-model occurrence;</li>
     *   <li>single-segment path — set directly on the named attribute of this type;</li>
     *   <li>no path (name absent from the content model, or a lenient recovery gave up) — bound by
     *       plain name matching, exactly as a non-routed type would.</li>
     * </ul>
     *
     * <p>XML attributes never enter routing: StAX reports them separately from child elements, so
     * unlike the Jackson path there is no need to filter attribute-named "fields" out of the input.
     */
    private Object readRoutedObject(XmlCursor cursor, TypeBinding binding, ContentModelRouter router)
            throws Exception {
        Object builder = BuilderAccess.newBuilder(binding);
        readXmlAttributesInto(cursor, binding, builder);

        List<BufferedChild> children = new ArrayList<BufferedChild>();
        StringBuilder textContent = new StringBuilder();
        while (cursor.hasNext()) {
            int event = cursor.next();
            if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                children.add(new BufferedChild(cursor.getLocalName(), cursor.getNamespaceURI(),
                        BufferedSubtree.capture(cursor)));
                // capture leaves the cursor on the child's END_ELEMENT.
            } else if (event == XMLStreamConstants.CHARACTERS
                    || event == XMLStreamConstants.CDATA) {
                textContent.append(cursor.getText());
            }
        }

        List<ContentModelRouter.Element> elements =
                new ArrayList<ContentModelRouter.Element>(children.size());
        for (BufferedChild child : children) {
            elements.add(new ContentModelRouter.Element(child.localName, child.namespaceUri));
        }
        ContentModelRouter.Route route = router.route(elements);

        Map<AttributeBinding, VirtualGroupState> virtualBuilders =
                new LinkedHashMap<AttributeBinding, VirtualGroupState>();
        List<VirtualPathAssembler.Assignment> virtualAssignments =
                new ArrayList<VirtualPathAssembler.Assignment>();

        for (int i = 0; i < children.size(); i++) {
            BufferedChild child = children.get(i);
            List<String> path = route.getPath(i);
            if (path == null) {
                handleChildElement(child.localName, child.subtree.cursor(), binding, builder,
                        virtualBuilders);
            } else if (path.size() == 1) {
                applyRoutedDirectElement(path.get(0), child, binding, builder);
            } else {
                virtualAssignments.add(new VirtualPathAssembler.Assignment(
                        path, route.getOccurrenceKey(i), child.subtree));
            }
        }

        applyValueContent(textContent, binding, builder);
        applyVirtualBuilders(virtualBuilders, builder);
        virtualPathAssembler.apply(builder, binding.getType(), virtualAssignments);

        return ((RosettaModelObjectBuilder) builder).build();
    }

    /**
     * Binds a child routed to a single-segment path: the path names a direct attribute of this type
     * by its logical Rune name, which may differ from the XML element name that got routed there.
     */
    private void applyRoutedDirectElement(String logicalName, BufferedChild child,
                                          TypeBinding binding, Object builder) throws Exception {
        AttributeBinding attr = BuilderAccess.findByLogicalName(binding, logicalName);
        if (attr == null) {
            return;
        }
        Class<?> concreteType = attr.getValueType();
        if (attr.getElementRef().isPresent()) {
            Class<?> substituted = substitutionResolver.resolve(attr.getElementRef().get(),
                    attr.getValueType(), child.localName, child.namespaceUri);
            if (substituted != null) {
                concreteType = substituted;
            }
        }
        applyChildElement(attr, concreteType, child.subtree.cursor(), builder);
    }

    /**
     * Reads one buffered leaf element as the value of {@code leafAttribute}. Supplied to
     * {@link VirtualPathAssembler} so routed leaves are deserialised by the same logic as any other
     * element.
     */
    private Object readLeafValue(AttributeBinding leafAttribute, BufferedSubtree value)
            throws Exception {
        XmlCursor cursor = value.cursor();
        if (leafAttribute.isRosettaModelObject()) {
            return readObject(cursor, leafAttribute.getValueType());
        }
        return converter.fromXmlString(cursor.getElementText(), leafAttribute.getValueType());
    }

    /** One buffered child element of a routed type: its name, namespace, and captured subtree. */
    private static final class BufferedChild {
        private final String localName;
        private final String namespaceUri;
        private final BufferedSubtree subtree;

        BufferedChild(String localName, String namespaceUri, BufferedSubtree subtree) {
            this.localName = localName;
            this.namespaceUri = namespaceUri;
            this.subtree = subtree;
        }
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
            XmlCursor cursor,
            TypeBinding binding,
            Object builder) throws Exception {
        int attrCount = cursor.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrLocalName = cursor.getAttributeLocalName(i);
            String attrValue = cursor.getAttributeValue(i);
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
            Map<AttributeBinding, VirtualGroupState> virtualBuilders) throws Exception {

        // Direct ATTRIBUTE bindings
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ATTRIBUTE) {
                continue;
            }
            if (attr.getXmlName().equals(attrLocalName)) {
                Object value = converter.fromXmlString(attrValue, attr.getValueType());
                BuilderAccess.apply(builder, attr, value);
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
                        BuilderAccess.apply(vBuilder, childAttr, value);
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
     * {@code binding} (direct ELEMENT or substitution-group ELEMENT) or one level into a
     * VIRTUAL type's bindings.
     *
     * <p>On return the cursor is positioned at the END_ELEMENT of the child element.
     */
    private void handleChildElement(
            String childLocalName,
            XmlCursor cursor,
            TypeBinding binding,
            Object builder,
            Map<AttributeBinding, VirtualGroupState> virtualBuilders) throws Exception {

        String childNamespaceURI = cursor.getNamespaceURI();

        // 1. Direct or substitution-group ELEMENT bindings
        ElementMatch match = resolveElementMatch(childLocalName, childNamespaceURI, binding);
        if (match != null) {
            applyChildElement(match.attribute, match.concreteType, cursor, builder);
            return;
        }

        // 2. One level into VIRTUAL types
        for (AttributeBinding virtualAttr : binding.getAttributes()) {
            if (virtualAttr.getXmlRepresentation() != AttributeXMLRepresentation.VIRTUAL) {
                continue;
            }
            TypeBinding virtualBinding = introspector.introspect(
                    virtualAttr.getValueType(), config);
            ElementMatch virtualMatch = resolveElementMatch(childLocalName, childNamespaceURI, virtualBinding);
            if (virtualMatch != null) {
                Object vBuilder = beginVirtualChild(virtualAttr, virtualMatch.attribute, virtualBuilders);
                applyChildElement(virtualMatch.attribute, virtualMatch.concreteType, cursor, vBuilder);
                return;
            }
        }

        // 3. Unknown child element — skip over it entirely
        skipElement(cursor);
    }

    /**
     * One attribute binding matched to a child element, together with the concrete type to
     * deserialise it as. For a direct (non-substitution) match, {@code concreteType} is simply
     * {@link AttributeBinding#getValueType()}. For a substitution-group match, it is the
     * polymorphic concrete type resolved by {@link SubstitutionResolver} from the element's
     * actual name/namespace (criteria 15/16, and the "@type"-driven-polymorphism case, e.g.
     * {@code testPolymorphicDeserialisation}).
     */
    private static final class ElementMatch {
        final AttributeBinding attribute;
        final Class<?> concreteType;

        ElementMatch(AttributeBinding attribute, Class<?> concreteType) {
            this.attribute = attribute;
            this.concreteType = concreteType;
        }
    }

    /**
     * Finds the ELEMENT-representation binding on {@code binding} that matches a child element
     * named {@code childLocalName} (namespace {@code childNamespaceURI}).
     *
     * <p>Direct (non-substitution) bindings are checked first, matching purely by local name —
     * the config carries no per-attribute namespace for direct elements (a Section 2-B gap, not
     * a Section 1 blocker). Substitution-group ({@code elementRef}) bindings are checked second:
     * {@link SubstitutionResolver} resolves the polymorphic concrete type by exact
     * namespace-qualified match first, local-name-only fallback second.
     */
    private ElementMatch resolveElementMatch(
            String childLocalName, String childNamespaceURI, TypeBinding binding) {
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ELEMENT) {
                continue;
            }
            if (!attr.getElementRef().isPresent() && attr.getXmlName().equals(childLocalName)) {
                return new ElementMatch(attr, attr.getValueType());
            }
        }
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() != AttributeXMLRepresentation.ELEMENT
                    || !attr.getElementRef().isPresent()) {
                continue;
            }
            Class<?> substituted = substitutionResolver.resolve(
                    attr.getElementRef().get(), attr.getValueType(), childLocalName, childNamespaceURI);
            if (substituted != null) {
                return new ElementMatch(attr, substituted);
            }
        }
        return null;
    }

    /**
     * Applies a matched child element's content to the target builder.
     *
     * <p>For nested Rune objects, recurses via {@link #readObject} using {@code concreteType}
     * (the polymorphic/substituted type when applicable, otherwise {@code attr.getValueType()}).
     * For scalars, reads the text content via {@link XmlCursor#getElementText()}.
     *
     * <p>On return the cursor is positioned at the END_ELEMENT of the child element.
     * This is guaranteed both when calling {@code readObject} (its contract) and when
     * calling {@code getElementText()} (per XMLStreamReader Javadoc).
     */
    private void applyChildElement(
            AttributeBinding attr,
            Class<?> concreteType,
            XmlCursor cursor,
            Object targetBuilder) throws Exception {
        if (attr.isRosettaModelObject()) {
            Object childObj = readObject(cursor, concreteType);
            BuilderAccess.apply(targetBuilder, attr, childObj);
        } else {
            // getElementText() reads text content and leaves the cursor on END_ELEMENT
            String text = cursor.getElementText();
            Object value = converter.fromXmlString(text, attr.getValueType());
            BuilderAccess.apply(targetBuilder, attr, value);
        }
    }

    /**
     * Sets the VALUE-representation text content on the builder, if the accumulated text is
     * non-blank and the type declares a VALUE attribute.
     */
    private void applyValueContent(
            StringBuilder textContent, TypeBinding binding, Object builder) throws Exception {
        String text = textContent.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        for (AttributeBinding attr : binding.getAttributes()) {
            if (attr.getXmlRepresentation() == AttributeXMLRepresentation.VALUE) {
                Object value = converter.fromXmlString(text, attr.getValueType());
                BuilderAccess.apply(builder, attr, value);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Builder helpers
    // -------------------------------------------------------------------------

    /**
     * Builds every occurrence accumulated for each virtual attribute and applies them to the
     * parent builder (adder for multi-cardinality virtual attributes — once per occurrence —
     * setter for single-cardinality ones).
     */
    private void applyVirtualBuilders(
            Map<AttributeBinding, VirtualGroupState> virtualBuilders, Object builder) throws Exception {
        for (Map.Entry<AttributeBinding, VirtualGroupState> entry : virtualBuilders.entrySet()) {
            AttributeBinding virtualAttr = entry.getKey();
            VirtualGroupState state = entry.getValue();
            if (state.currentBuilder != null) {
                state.completedValues.add(((RosettaModelObjectBuilder) state.currentBuilder).build());
            }
            for (Object value : state.completedValues) {
                BuilderAccess.apply(builder, virtualAttr, value);
            }
        }
    }

    /**
     * Returns the current (or first) virtual builder for {@code virtualAttr}, creating one if
     * absent. Used for XML attributes, which are read once from the parent START_ELEMENT before
     * any child element can start a new occurrence, so no occurrence-boundary check applies here.
     */
    private Object getOrCreateVirtualBuilder(
            AttributeBinding virtualAttr,
            Map<AttributeBinding, VirtualGroupState> virtualBuilders) throws Exception {
        VirtualGroupState state = virtualBuilders.get(virtualAttr);
        if (state == null) {
            state = new VirtualGroupState();
            virtualBuilders.put(virtualAttr, state);
        }
        if (state.currentBuilder == null) {
            state.currentBuilder = BuilderAccess.newBuilder(
                    introspector.introspect(virtualAttr.getValueType(), config));
        }
        return state.currentBuilder;
    }

    /**
     * Returns the virtual builder that {@code childAttr}'s value should be written into, handling
     * occurrence boundaries for a repeated unwrapped group (issue 7 / criterion 17).
     *
     * <p>A multi-cardinality VIRTUAL attribute (e.g. {@code <group ref maxOccurs="unbounded">})
     * has no wrapper element, so its repeated instances appear back-to-back as the same sequence
     * of child element names, e.g. {@code <c/><d/><c/><d/>} for two occurrences of a group
     * {@code {c, d}}. With no content model available (Section 1's design constraint — structure
     * comes from bean declaration order), the only signal available is: a single-cardinality slot
     * being filled a second time in the current occurrence means a new occurrence has begun.
     * Multi-cardinality child attributes never trigger this — they accumulate freely within one
     * occurrence, exactly like {@link BuilderAccess#apply}'s adder semantics.
     *
     * <p>Tracked by {@code childAttr}'s logical name rather than the binding instance itself:
     * {@link RuneTypeIntrospector#introspect} builds a fresh {@code AttributeBinding} on every
     * call (no caching), and the virtual type is re-introspected for every child element, so two
     * bindings for the same logical attribute across occurrences are never {@code ==} or
     * {@code equals} to each other.
     */
    private Object beginVirtualChild(
            AttributeBinding virtualAttr,
            AttributeBinding childAttr,
            Map<AttributeBinding, VirtualGroupState> virtualBuilders) throws Exception {
        VirtualGroupState state = virtualBuilders.get(virtualAttr);
        if (state == null) {
            state = new VirtualGroupState();
            virtualBuilders.put(virtualAttr, state);
        }
        boolean startsNewOccurrence = virtualAttr.isMulti()
                && state.currentBuilder != null
                && !childAttr.isMulti()
                && state.filledInCurrentOccurrence.contains(childAttr.getLogicalName());
        if (startsNewOccurrence) {
            state.completedValues.add(((RosettaModelObjectBuilder) state.currentBuilder).build());
            state.currentBuilder = null;
            state.filledInCurrentOccurrence.clear();
        }
        if (state.currentBuilder == null) {
            state.currentBuilder = BuilderAccess.newBuilder(
                    introspector.introspect(virtualAttr.getValueType(), config));
        }
        if (!childAttr.isMulti()) {
            state.filledInCurrentOccurrence.add(childAttr.getLogicalName());
        }
        return state.currentBuilder;
    }

    /**
     * Accumulator for one VIRTUAL attribute's occurrences while reading a parent element: a
     * builder for the occurrence currently being filled, the logical names of single-cardinality
     * child attributes already filled in it (used to detect occurrence boundaries — see
     * {@link #beginVirtualChild}), and every already completed (built) occurrence in document
     * order.
     */
    private static final class VirtualGroupState {
        private final List<Object> completedValues = new ArrayList<Object>();
        private final Set<String> filledInCurrentOccurrence = new HashSet<String>();
        private Object currentBuilder;
    }

    // -------------------------------------------------------------------------
    // Skip and prune utilities
    // -------------------------------------------------------------------------

    /**
     * Skips past an element the cursor is currently positioned on (START_ELEMENT).
     * Handles nested elements. On return the cursor is positioned at the END_ELEMENT
     * of the skipped element.
     */
    private void skipElement(XmlCursor cursor) throws Exception {
        int depth = 1;
        while (cursor.hasNext() && depth > 0) {
            int event = cursor.next();
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
