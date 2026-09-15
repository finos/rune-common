package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection helpers for materializing virtual object graphs from routed XML content-model
 * assignments and attaching them to a parent Rosetta builder.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>multi-layer virtual paths ({@code outer.inner.leaf}, etc.),</li>
 *   <li>several leaves contributed by the same content-model occurrence merged into one virtual
 *       object,</li>
 *   <li>multi-cardinality and single-cardinality attributes at every layer.</li>
 * </ul>
 */
final class VirtualPathBuilderHelper {

    private final Map<CacheKey, AttributeAccessor> attributeAccessorCache = new ConcurrentHashMap<>();

    /**
     * Apply the supplied routed assignments to the parent builder instance.
     *
     * @param parentBuilder Rosetta builder of the parent type (the object being deserialised).
     * @param assignments   ordered list of routed leaf assignments produced by the matcher and the
     *                      buffered values for those leaves.
     */
    void applyAssignments(Object parentBuilder,
                          List<RoutedAssignment> assignments,
                          DeserializationContext ctxt) throws IOException {
        if (assignments.isEmpty()) {
            return;
        }
        // 1. Build a tree grouped by occurrence and path-prefix.
        VirtualNode root = new VirtualNode(null, null, null);
        for (RoutedAssignment a : assignments) {
            root.add(a);
        }
        // 2. Recursively build and attach each top-level virtual object.
        for (VirtualNode topAttribute : root.childrenByAttribute.values()) {
            attachAttribute(parentBuilder, topAttribute, ctxt);
        }
    }

    private void attachAttribute(Object parentBuilder,
                                 VirtualNode attributeNode,
                                 DeserializationContext ctxt) throws IOException {
        String attribute = attributeNode.attributeName;
        // Each entry in childrenByOccurrence is one repetition of the virtual attribute.
        for (VirtualNode occurrence : attributeNode.childrenByOccurrence) {
            Object built = materialise(parentBuilder.getClass(), attribute, occurrence, ctxt);
            invokeAttach(parentBuilder, attribute, built);
        }
    }

    private Object materialise(Class<?> ownerBuilderClass,
                               String attribute,
                               VirtualNode occurrence,
                               DeserializationContext ctxt) throws IOException {
        // Find the attribute Rosetta type via the parent's add* or set* method signature.
        AttributeAccessor accessor = lookupAccessor(ownerBuilderClass, attribute);
        Class<?> attributeType = accessor.parameterType;
        // Get builder for that attribute type via static builder() method.
        Object subBuilder;
        try {
            Method builderFactory = attributeType.getMethod("builder");
            if (!Modifier.isStatic(builderFactory.getModifiers())) {
                throw new IOException("Expected static builder() on " + attributeType.getName());
            }
            subBuilder = builderFactory.invoke(null);
        } catch (NoSuchMethodException e) {
            throw new IOException("No static builder() on " + attributeType.getName(), e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Failed to invoke builder() on " + attributeType.getName(), e);
        }
        // Apply leaf values.
        for (Map.Entry<String, BufferedLeaf> leaf : occurrence.leafValues.entrySet()) {
            applyLeaf(subBuilder, leaf.getKey(), leaf.getValue(), ctxt);
        }
        // Recurse into nested virtual attributes.
        for (VirtualNode child : occurrence.childrenByAttribute.values()) {
            attachAttribute(subBuilder, child, ctxt);
        }
        // Build the sub-object.
        try {
            Method buildMethod = subBuilder.getClass().getMethod("build");
            return buildMethod.invoke(subBuilder);
        } catch (NoSuchMethodException e) {
            throw new IOException("No build() on " + subBuilder.getClass().getName(), e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Failed to invoke build() on " + subBuilder.getClass().getName(), e);
        }
    }

    private void applyLeaf(Object builder, String leafAttribute, BufferedLeaf leaf,
                           DeserializationContext ctxt) throws IOException {
        AttributeAccessor accessor = lookupAccessor(builder.getClass(), leafAttribute);
        Class<?> leafType = accessor.parameterType;
        JavaType javaType = ctxt.constructType(leafType);
        com.fasterxml.jackson.databind.JsonDeserializer<Object> deser = ctxt.findRootValueDeserializer(javaType);
        Object value;
        try (JsonParser parser = leaf.value.asParserOnFirstToken()) {
            value = deser.deserialize(parser, ctxt);
        }
        invokeAttach(builder, leafAttribute, value);
    }

    private void invokeAttach(Object builder, String attribute, Object value) throws IOException {
        AttributeAccessor accessor = lookupAccessor(builder.getClass(), attribute);
        try {
            accessor.method.invoke(builder, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Failed to apply attribute '" + attribute + "' on "
                    + builder.getClass().getName(), e);
        }
    }

    /**
     * One leaf value produced by the deserializer wrapper for a virtual path. The {@code value} is
     * a copy of the original XML child subtree (XML attributes and text preserved).
     */
    static final class BufferedLeaf {
        final TokenBuffer value;

        BufferedLeaf(TokenBuffer value) {
            this.value = value;
        }
    }

    /**
     * One routed leaf assignment: full Rosetta property path (e.g. {@code [outer, inner, leaf]}),
     * occurrence key identifying which content-model repetition produced it, and the buffered
     * leaf value.
     */
    static final class RoutedAssignment {
        final List<String> path;
        final String occurrenceKey;
        final BufferedLeaf value;

        RoutedAssignment(List<String> path, String occurrenceKey, BufferedLeaf value) {
            this.path = path;
            this.occurrenceKey = occurrenceKey;
            this.value = value;
        }
    }

    private static final class VirtualNode {
        final String attributeName;
        final String occurrenceKey;
        final VirtualNode parent;
        final Map<String, BufferedLeaf> leafValues = new LinkedHashMap<>();
        // For each child virtual attribute, a list of occurrences (each its own VirtualNode).
        final Map<String, VirtualNode> childrenByAttribute = new LinkedHashMap<>();
        // For nodes that themselves represent an attribute, this is the ordered list of repetitions.
        final List<VirtualNode> childrenByOccurrence = new ArrayList<>();

        VirtualNode(String attributeName, String occurrenceKey, VirtualNode parent) {
            this.attributeName = attributeName;
            this.occurrenceKey = occurrenceKey;
            this.parent = parent;
        }

        void add(RoutedAssignment assignment) {
            addRecursive(assignment.path, 0, assignment.occurrenceKey, assignment.value);
        }

        private void addRecursive(List<String> path, int depth, String occurrenceKey, BufferedLeaf value) {
            String attribute = path.get(depth);
            boolean lastSegment = (depth == path.size() - 1);
            if (lastSegment) {
                // Should never reach here on the root: leaf segments are always inside an attribute node.
                throw new IllegalStateException("Path with only one segment is not a virtual path");
            }
            VirtualNode attributeBucket = childrenByAttribute.get(attribute);
            if (attributeBucket == null) {
                attributeBucket = new VirtualNode(attribute, null, this);
                childrenByAttribute.put(attribute, attributeBucket);
            }
            // Find or create the occurrence for this attribute.
            VirtualNode occurrence = null;
            if (!attributeBucket.childrenByOccurrence.isEmpty()) {
                VirtualNode last = attributeBucket.childrenByOccurrence.get(attributeBucket.childrenByOccurrence.size() - 1);
                if (last.occurrenceKey != null && last.occurrenceKey.equals(occurrenceKey)) {
                    occurrence = last;
                }
            }
            if (occurrence == null) {
                occurrence = new VirtualNode(attribute, occurrenceKey, attributeBucket);
                attributeBucket.childrenByOccurrence.add(occurrence);
            }
            if (depth == path.size() - 2) {
                // Last segment is a leaf within the current occurrence.
                String leafName = path.get(path.size() - 1);
                if (occurrence.leafValues.containsKey(leafName)) {
                    // Same leaf appearing twice in the same occurrence: start a new occurrence.
                    occurrence = new VirtualNode(attribute, occurrenceKey + "#" + attributeBucket.childrenByOccurrence.size(), attributeBucket);
                    attributeBucket.childrenByOccurrence.add(occurrence);
                }
                occurrence.leafValues.put(leafName, value);
            } else {
                occurrence.addRecursive(path, depth + 1, occurrenceKey, value);
            }
        }
    }

    /**
     * Resolved Rosetta attribute access: a single-argument {@code addX}/{@code setX} method on the
     * builder type, plus the attribute value type.
     */
    private AttributeAccessor lookupAccessor(Class<?> builderClass, String attribute) throws IOException {
        CacheKey key = new CacheKey(builderClass, attribute);
        AttributeAccessor cached = attributeAccessorCache.get(key);
        if (cached != null) {
            return cached;
        }
        AttributeAccessor resolved = AttributeAccessor.resolve(builderClass, attribute);
        AttributeAccessor previous = attributeAccessorCache.putIfAbsent(key, resolved);
        return previous != null ? previous : resolved;
    }

    private static final class AttributeAccessor {
        final Method method;
        final Class<?> parameterType;

        private AttributeAccessor(Method method, Class<?> parameterType) {
            this.method = method;
            this.parameterType = parameterType;
        }

        private static AttributeAccessor resolve(Class<?> builderClass, String attribute) throws IOException {
            String capitalised = capitalize(attribute);
            // Prefer single-argument add<Attr>(value) where the parameter is NOT a List.
            Method bestAdd = null;
            Method bestSet = null;
            for (Method method : builderClass.getMethods()) {
                if (method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> param = method.getParameterTypes()[0];
                if (List.class.isAssignableFrom(param)) {
                    continue;
                }
                if (method.getName().equals("add" + capitalised)) {
                    bestAdd = preferMoreSpecific(bestAdd, method);
                } else if (method.getName().equals("set" + capitalised)) {
                    bestSet = preferMoreSpecific(bestSet, method);
                }
            }
            Method chosen = bestAdd != null ? bestAdd : bestSet;
            if (chosen == null) {
                throw new IOException("Could not find add" + capitalised + " or set" + capitalised
                        + "(value) on " + builderClass.getName());
            }
            chosen.setAccessible(true);
            return new AttributeAccessor(chosen, chosen.getParameterTypes()[0]);
        }

        private static Method preferMoreSpecific(Method current, Method candidate) {
            if (current == null) {
                return candidate;
            }
            Class<?> currentType = current.getParameterTypes()[0];
            Class<?> candidateType = candidate.getParameterTypes()[0];
            if (currentType.isAssignableFrom(candidateType) && !candidateType.equals(currentType)) {
                return candidate;
            }
            return current;
        }

        private static String capitalize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
        }

    }

    private static final class CacheKey {
        final Class<?> builderClass;
        final String attribute;

        CacheKey(Class<?> builderClass, String attribute) {
            this.builderClass = builderClass;
            this.attribute = attribute;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey k = (CacheKey) o;
            return builderClass == k.builderClass && attribute.equals(k.attribute);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(builderClass) * 31 + attribute.hashCode();
        }
    }

}
