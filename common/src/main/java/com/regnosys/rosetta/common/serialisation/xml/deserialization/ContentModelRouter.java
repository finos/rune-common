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

import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModelNodeType;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.XMLContentModelMatcher.OccurrenceKey;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.XMLContentModelMatcher.RoutingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Public, format-agnostic entry point to the XML content-model routing algorithm.
 *
 * <p>This is the seam that lets the StAX binder reuse {@link XMLContentModelMatcher} — a pure
 * routing algorithm with no Jackson dependency — without exposing the Jackson-era
 * {@link RoutingInput} / {@link RoutingResult} types (which live in this package). A caller supplies
 * the parent element's child elements in document order (local name + namespace URI) and gets back,
 * for each child, the Rosetta property path it belongs to and an opaque key identifying which
 * content-model repetition produced it.</p>
 *
 * <p>Namespace state supplied here is always real: a {@code null}/empty namespace URI means the
 * element genuinely has no namespace, and is matched as
 * {@link RoutingInput.Namespace#absent()} — never as
 * {@link RoutingInput.Namespace#unknown()}. The {@code UNKNOWN} state exists only because Jackson's
 * {@code TokenBuffer} discards the StAX namespace context before the matcher runs, forcing a
 * degraded local-name fallback. A StAX-native reader has the namespace in hand, so a content model
 * that requires a namespace correctly rejects an element from a different (or no) namespace.</p>
 *
 * <p>The lenient-recovery policy (reorder, then drop un-routable names, then give up) mirrors
 * {@code XMLContentModelDisambiguatingDeserializer}, so both engines behave identically on
 * documents that do not strictly match their content model.</p>
 */
public final class ContentModelRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentModelRouter.class);

    private final XMLContentModel contentModel;
    private final String ownerDescription;
    private final Set<String> modelElementNames;
    private final boolean hasAnyNode;
    // Canonical document-order position of each element name (first occurrence in a DFS of the
    // content model). Used to stably reorder misordered input before a lenient re-match.
    private final Map<String, Integer> canonicalPosition = new HashMap<>();
    // Element names that map to exactly one path, hence routable regardless of order.
    private final Set<String> uniquelyRoutableNames = new HashSet<>();

    /**
     * @param contentModel     the content model to route against
     * @param ownerDescription human-readable name of the owning type, used in warning messages
     */
    public ContentModelRouter(XMLContentModel contentModel, String ownerDescription) {
        this.contentModel = contentModel;
        this.ownerDescription = ownerDescription;
        this.modelElementNames = collectXmlNames(contentModel);
        this.hasAnyNode = containsAnyNode(contentModel);
        indexElementNames(contentModel);
    }

    /**
     * Returns {@code true} when the supplied content model requires any disambiguation work at all.
     * If every path is direct and single-segment AND the model contains no duplicate XML names,
     * plain name-based routing already produces the correct result and the caller can skip the
     * buffering overhead entirely.
     */
    public static boolean requiresRouting(XMLContentModel contentModel) {
        Set<String> seen = new HashSet<>();
        final boolean[] hasNested = {false};
        final boolean[] hasDuplicate = {false};
        walk(contentModel, node -> {
            if (node.getNodeType() == XMLContentModelNodeType.ELEMENT
                    || node.getNodeType() == XMLContentModelNodeType.ANY) {
                node.getPath().ifPresent(path -> {
                    if (path.size() > 1) {
                        hasNested[0] = true;
                    }
                });
                node.getXmlName().ifPresent(name -> {
                    if (!seen.add(name)) {
                        hasDuplicate[0] = true;
                    }
                });
            }
        });
        return hasNested[0] || hasDuplicate[0];
    }

    /**
     * Routes {@code children} (all child elements of one parent, in document order).
     *
     * <p>Children whose name plays no part in the content model are left unrouted (the caller
     * should fall back to plain name-based binding for those). Strict ordered matching is tried
     * first; on failure the lenient recovery policy applies.</p>
     *
     * @return the routing decision, keyed by index into {@code children}
     */
    public Route route(List<Element> children) {
        List<RoutingInput> inputs = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            if (!participatesInRouting(child.getXmlName())) {
                continue;
            }
            inputs.add(new RoutingInput(i, child.getXmlName(), toMatcherNamespace(child)));
        }
        if (inputs.isEmpty()) {
            return Route.empty();
        }

        RoutingResult routing = XMLContentModelMatcher.route(contentModel, inputs);
        if (routing.getStatus() != RoutingResult.Status.SUCCESS) {
            routing = lenientRoute(inputs);
        }
        return toRoute(routing, inputs);
    }

    /**
     * A named element takes part in routing only if the content model mentions it. A model
     * containing an {@code ANY} wildcard must see every child element so the wildcard has
     * something to consume.
     */
    private boolean participatesInRouting(String xmlName) {
        return hasAnyNode || modelElementNames.contains(xmlName);
    }

    private static RoutingInput.Namespace toMatcherNamespace(Element element) {
        String namespaceUri = element.getNamespaceUri();
        if (namespaceUri == null || namespaceUri.isEmpty()) {
            return RoutingInput.Namespace.absent();
        }
        return RoutingInput.Namespace.present(namespaceUri);
    }

    private Route toRoute(RoutingResult routing, List<RoutingInput> inputs) {
        Map<Integer, List<String>> pathByIndex = new LinkedHashMap<>();
        Map<Integer, Object> occurrenceByIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : routing.getPathByIndex().entrySet()) {
            int childIndex = inputs.get(entry.getKey()).getFieldIndex();
            pathByIndex.put(childIndex, entry.getValue());
            occurrenceByIndex.put(childIndex, routing.getOccurrenceByIndex().get(entry.getKey()));
        }
        return new Route(pathByIndex, occurrenceByIndex);
    }

    // -------------------------------------------------------------------------
    // Lenient recovery (ported from XMLContentModelDisambiguatingDeserializer)
    // -------------------------------------------------------------------------

    /**
     * Best-effort routing used when strict, ordered matching fails. The strategy, in order:
     * <ol>
     *   <li>the elements may merely be out of schema order, so stably reorder them into canonical
     *       content-model order and re-match (this reuses the matcher's occurrence handling);</li>
     *   <li>otherwise drop elements whose name cannot be uniquely routed and re-match the rest;</li>
     *   <li>otherwise skip all content-model routing.</li>
     * </ol>
     * Any element not present in the returned routing is left for the caller to bind by name.
     * Every recovery path emits a WARN.
     */
    private RoutingResult lenientRoute(List<RoutingInput> inputs) {
        // Attempt 1: treat as a pure ordering problem.
        RoutingResult reordered = routeReordered(inputs, inputs);
        if (reordered != null) {
            LOGGER.warn("XML content for {} was not in schema order; elements were reordered to deserialize. "
                    + "XML child sequence: {}", ownerDescription, xmlSequence(inputs));
            return reordered;
        }
        // Attempt 2: keep only uniquely-routable elements, skip the rest.
        List<RoutingInput> keep = new ArrayList<>();
        List<RoutingInput> dropped = new ArrayList<>();
        for (RoutingInput input : inputs) {
            if (uniquelyRoutableNames.contains(input.getXmlName())) {
                keep.add(input);
            } else {
                dropped.add(input);
            }
        }
        if (!dropped.isEmpty() && !keep.isEmpty()) {
            RoutingResult partial = routeReordered(keep, inputs);
            if (partial != null) {
                LOGGER.warn("Skipped {} XML element(s) of {} that could not be routed by the content model: {}",
                        dropped.size(), ownerDescription, xmlSequence(dropped));
                return partial;
            }
        }
        // Attempt 3: give up on content-model routing entirely.
        LOGGER.warn("Cannot route XML content for {}. XML child sequence: {}. "
                        + "Skipping content-model routing; un-routable elements are bound by name.",
                ownerDescription, xmlSequence(inputs));
        return RoutingResult.success(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Stably reorder {@code subset} into canonical content-model order, run the strict matcher, and
     * if it succeeds remap the routing back to indices in {@code originalInputs}. Returns
     * {@code null} when the reordered subset still does not match uniquely.
     */
    private RoutingResult routeReordered(List<RoutingInput> subset, List<RoutingInput> originalInputs) {
        List<RoutingInput> sorted = new ArrayList<>(subset);
        // List.sort is stable, so elements sharing an XML name keep their original relative order
        // (preserving e.g. the order of repeated choice entries).
        sorted.sort(Comparator.comparingInt(
                input -> canonicalPosition.getOrDefault(input.getXmlName(), Integer.MAX_VALUE)));

        RoutingResult result = XMLContentModelMatcher.route(contentModel, sorted);
        if (result.getStatus() != RoutingResult.Status.SUCCESS) {
            return null;
        }

        IdentityHashMap<RoutingInput, Integer> originalIndex = new IdentityHashMap<>();
        for (int i = 0; i < originalInputs.size(); i++) {
            originalIndex.put(originalInputs.get(i), i);
        }
        Map<Integer, List<String>> pathByIndex = new LinkedHashMap<>();
        Map<Integer, OccurrenceKey> occurrenceByIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : result.getPathByIndex().entrySet()) {
            RoutingInput input = sorted.get(entry.getKey());
            int original = originalIndex.get(input);
            pathByIndex.put(original, entry.getValue());
            occurrenceByIndex.put(original, result.getOccurrenceByIndex().get(entry.getKey()));
        }
        return RoutingResult.success(pathByIndex, occurrenceByIndex);
    }

    // -------------------------------------------------------------------------
    // Content-model indexing
    // -------------------------------------------------------------------------

    private void indexElementNames(XMLContentModel root) {
        Map<String, Set<List<String>>> pathsByName = new HashMap<>();
        final int[] counter = {0};
        walk(root, node -> {
            if (node.getNodeType() != XMLContentModelNodeType.ELEMENT
                    && node.getNodeType() != XMLContentModelNodeType.ANY) {
                return;
            }
            node.getXmlName().ifPresent(name -> {
                canonicalPosition.putIfAbsent(name, counter[0]);
                node.getPath().ifPresent(path ->
                        pathsByName.computeIfAbsent(name, k -> new HashSet<>()).add(path));
            });
            counter[0]++;
        });
        pathsByName.forEach((name, paths) -> {
            if (paths.size() == 1) {
                uniquelyRoutableNames.add(name);
            }
        });
    }

    private static Set<String> collectXmlNames(XMLContentModel root) {
        Set<String> names = new LinkedHashSet<>();
        walk(root, node -> {
            if (node.getNodeType() == XMLContentModelNodeType.ELEMENT) {
                node.getXmlName().ifPresent(names::add);
            }
        });
        return names;
    }

    private static boolean containsAnyNode(XMLContentModel root) {
        final boolean[] result = {false};
        walk(root, node -> {
            if (node.getNodeType() == XMLContentModelNodeType.ANY) {
                result[0] = true;
            }
        });
        return result[0];
    }

    private static void walk(XMLContentModel node, Consumer<XMLContentModel> visitor) {
        visitor.accept(node);
        node.getChildren().ifPresent(children -> {
            for (XMLContentModel child : children) {
                walk(child, visitor);
            }
        });
    }

    private static String xmlSequence(List<RoutingInput> inputs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(inputs.get(i).getXmlName());
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Public value types
    // -------------------------------------------------------------------------

    /**
     * One child element of the parent being routed: its XML local name and namespace URI, in
     * document order. A {@code null} or empty {@code namespaceUri} means "no namespace" (known
     * absent), never "unknown".
     */
    public static final class Element {
        private final String xmlName;
        private final String namespaceUri;

        public Element(String xmlName, String namespaceUri) {
            this.xmlName = xmlName;
            this.namespaceUri = namespaceUri;
        }

        public String getXmlName() {
            return xmlName;
        }

        public String getNamespaceUri() {
            return namespaceUri;
        }

        @Override
        public String toString() {
            return namespaceUri == null || namespaceUri.isEmpty()
                    ? xmlName
                    : "{" + namespaceUri + "}" + xmlName;
        }
    }

    /**
     * The routing decision for one parent element, keyed by index into the child list supplied to
     * {@link #route(List)}. An index with no path was not routed by the content model and should be
     * bound by name instead.
     */
    public static final class Route {
        private static final Route EMPTY = new Route(Collections.emptyMap(), Collections.emptyMap());

        private final Map<Integer, List<String>> pathByIndex;
        private final Map<Integer, Object> occurrenceByIndex;

        private Route(Map<Integer, List<String>> pathByIndex, Map<Integer, Object> occurrenceByIndex) {
            this.pathByIndex = pathByIndex;
            this.occurrenceByIndex = occurrenceByIndex;
        }

        static Route empty() {
            return EMPTY;
        }

        /**
         * The Rosetta property path for the child at {@code childIndex}, or {@code null} if the
         * content model did not route it. A single-segment path is a direct property of the parent;
         * a multi-segment path walks through virtual (unwrapped) intermediate types.
         */
        public List<String> getPath(int childIndex) {
            return pathByIndex.get(childIndex);
        }

        /**
         * Opaque identifier of the content-model repetition that produced the child's assignment.
         * Two children sharing an occurrence key belong to the same virtual object; the key is only
         * ever compared with {@link Object#equals(Object)}.
         */
        public Object getOccurrenceKey(int childIndex) {
            return occurrenceByIndex.get(childIndex);
        }

        public boolean isEmpty() {
            return pathByIndex.isEmpty();
        }
    }
}
