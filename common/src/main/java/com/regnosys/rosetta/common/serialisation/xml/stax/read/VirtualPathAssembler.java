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

import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.AttributeBinding;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.RuneTypeIntrospector;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.TypeBinding;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Materialises the virtual (unwrapped) object graph described by routed content-model assignments
 * and attaches it to the parent builder.
 *
 * <p>The StAX-native counterpart to the Jackson-era {@code VirtualPathBuilderHelper}. The occurrence
 * grouping is the same proven algorithm: leaves contributed by one content-model repetition are
 * merged into a single virtual object, and each repetition produces its own object. Two things
 * differ:</p>
 * <ul>
 *   <li>Attribute access goes through {@link RuneTypeIntrospector}, so setters/adders and value
 *       types come from the same binding plan the rest of the binder uses — no name-guessing over
 *       {@code addX}/{@code setX} reflection.</li>
 *   <li>Occurrence identity is the matcher's own key compared by {@link Object#equals(Object)},
 *       rather than its {@code toString()}.</li>
 * </ul>
 *
 * <p>Supports multi-layer paths ({@code outer.inner.leaf}), several leaves per occurrence, and
 * multi- or single-cardinality attributes at every layer.</p>
 */
final class VirtualPathAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualPathAssembler.class);

    private final RuneTypeIntrospector introspector;
    private final RosettaXMLConfiguration config;
    private final LeafValueReader leafValueReader;

    VirtualPathAssembler(RuneTypeIntrospector introspector,
                         RosettaXMLConfiguration config,
                         LeafValueReader leafValueReader) {
        this.introspector = introspector;
        this.config = config;
        this.leafValueReader = leafValueReader;
    }

    /**
     * Reads one buffered leaf element into the value of {@code leafAttribute}. Implemented by
     * {@link StaxReader} so that leaf objects are deserialised by the very same reader logic as any
     * other element (nested objects, XML attributes, text content and all).
     */
    interface LeafValueReader {
        Object read(AttributeBinding leafAttribute, BufferedSubtree value) throws Exception;
    }

    /** One routed leaf: the full Rosetta property path, its occurrence key, and the buffered element. */
    static final class Assignment {
        private final List<String> path;
        private final Object occurrenceKey;
        private final BufferedSubtree value;

        Assignment(List<String> path, Object occurrenceKey, BufferedSubtree value) {
            this.path = path;
            this.occurrenceKey = occurrenceKey;
            this.value = value;
        }
    }

    /**
     * Applies {@code assignments} (in document order) to {@code parentBuilder}, an instance of the
     * builder for {@code parentType}.
     */
    void apply(Object parentBuilder, Class<?> parentType, List<Assignment> assignments) throws Exception {
        if (assignments.isEmpty()) {
            return;
        }
        Node root = new Node(null, null);
        for (Assignment assignment : assignments) {
            root.add(assignment);
        }
        for (Node attributeNode : root.childrenByAttribute.values()) {
            attach(parentBuilder, parentType, attributeNode);
        }
    }

    private void attach(Object ownerBuilder, Class<?> ownerType, Node attributeNode) throws Exception {
        TypeBinding ownerBinding = introspector.introspect(ownerType, config);
        AttributeBinding attribute = BuilderAccess.findByLogicalName(ownerBinding, attributeNode.attributeName);
        if (attribute == null) {
            LOGGER.warn("Content model routes into '{}' but {} has no such attribute; skipping",
                    attributeNode.attributeName, ownerType.getName());
            return;
        }
        Class<?> attributeType = attribute.getValueType();
        TypeBinding attributeBinding = introspector.introspect(attributeType, config);

        for (Node occurrence : attributeNode.childrenByOccurrence) {
            Object subBuilder = BuilderAccess.newBuilder(attributeBinding);
            for (Map.Entry<String, BufferedSubtree> leaf : occurrence.leafValues.entrySet()) {
                AttributeBinding leafAttribute =
                        BuilderAccess.findByLogicalName(attributeBinding, leaf.getKey());
                if (leafAttribute == null) {
                    LOGGER.warn("Content model routes to leaf '{}' but {} has no such attribute; skipping",
                            leaf.getKey(), attributeType.getName());
                    continue;
                }
                BuilderAccess.apply(subBuilder, leafAttribute,
                        leafValueReader.read(leafAttribute, leaf.getValue()));
            }
            for (Node nested : occurrence.childrenByAttribute.values()) {
                attach(subBuilder, attributeType, nested);
            }
            BuilderAccess.apply(ownerBuilder, attribute,
                    ((RosettaModelObjectBuilder) subBuilder).build());
        }
    }

    /**
     * A node in the grouped assignment tree: either an attribute bucket (holding one child node per
     * occurrence) or a single occurrence (holding its leaf values and any nested attribute buckets).
     */
    private static final class Node {
        private final String attributeName;
        private final Object occurrenceKey;
        private final Map<String, BufferedSubtree> leafValues = new LinkedHashMap<>();
        private final Map<String, Node> childrenByAttribute = new LinkedHashMap<>();
        private final List<Node> childrenByOccurrence = new ArrayList<>();

        Node(String attributeName, Object occurrenceKey) {
            this.attributeName = attributeName;
            this.occurrenceKey = occurrenceKey;
        }

        void add(Assignment assignment) {
            addRecursive(assignment.path, 0, assignment.occurrenceKey, assignment.value);
        }

        private void addRecursive(List<String> path, int depth, Object occurrenceKey, BufferedSubtree value) {
            if (depth == path.size() - 1) {
                throw new IllegalStateException("Path with only one segment is not a virtual path: " + path);
            }
            String attribute = path.get(depth);
            Node attributeBucket = childrenByAttribute.get(attribute);
            if (attributeBucket == null) {
                attributeBucket = new Node(attribute, null);
                childrenByAttribute.put(attribute, attributeBucket);
            }
            // Reuse the current (last) occurrence only when it came from the same content-model
            // repetition; otherwise start a new virtual object.
            Node occurrence = null;
            if (!attributeBucket.childrenByOccurrence.isEmpty()) {
                Node last = attributeBucket.childrenByOccurrence
                        .get(attributeBucket.childrenByOccurrence.size() - 1);
                if (last.occurrenceKey != null && Objects.equals(last.occurrenceKey, occurrenceKey)) {
                    occurrence = last;
                }
            }
            if (occurrence == null) {
                occurrence = new Node(attribute, occurrenceKey);
                attributeBucket.childrenByOccurrence.add(occurrence);
            }
            if (depth == path.size() - 2) {
                String leafName = path.get(path.size() - 1);
                if (occurrence.leafValues.containsKey(leafName)) {
                    // Same leaf twice within one occurrence: the model cannot mean one object, so
                    // start a fresh occurrence that no later assignment can join.
                    occurrence = new Node(attribute, new Object());
                    attributeBucket.childrenByOccurrence.add(occurrence);
                }
                occurrence.leafValues.put(leafName, value);
            } else {
                occurrence.addRecursive(path, depth + 1, occurrenceKey, value);
            }
        }
    }
}
