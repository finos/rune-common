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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure-data, recursive matcher of an XML content-model tree against an ordered list of
 * {@link RoutingInput input items}. Used by {@link XMLContentModelDisambiguatingDeserializer} to
 * decide where each ambiguous XML child element should be routed.
 *
 * <p>Supports {@code ELEMENT}, {@code SEQUENCE}, {@code CHOICE}, {@code ALL} and a minimal form
 * of {@code ANY}. Occurrence bounds ({@code minOccurs}/{@code maxOccurs}) are honored.</p>
 *
 * <p>Top-level tie-breaking is implemented by {@link #route(XMLContentModel, List)}: it keeps
 * only the matches that consume the entire input and that have exactly one resolution.</p>
 */
final class XMLContentModelMatcher {

    private XMLContentModelMatcher() {
    }

    /**
     * Match the supplied {@code root} content-model against {@code inputs} and return the unique
     * routing assignment that consumes the entire input.
     */
    static RoutingResult route(XMLContentModel root, List<RoutingInput> inputs) {
        List<MatchResult> all = match(root, inputs, 0, new OccurrenceStack());
        List<MatchResult> complete = new ArrayList<>();
        for (MatchResult mr : all) {
            if (mr.nextIndex == inputs.size()) {
                complete.add(mr);
            }
        }
        if (complete.isEmpty()) {
            return RoutingResult.noMatch();
        }
        // Deduplicate by assignment content.
        List<MatchResult> unique = new ArrayList<>();
        outer:
        for (MatchResult mr : complete) {
            for (MatchResult existing : unique) {
                if (assignmentsEqual(existing.assignments, mr.assignments)) {
                    continue outer;
                }
            }
            unique.add(mr);
        }
        if (unique.size() > 1) {
            return RoutingResult.ambiguous(unique);
        }
        MatchResult winner = unique.get(0);
        Map<Integer, List<String>> pathByIndex = new LinkedHashMap<>();
        Map<Integer, OccurrenceKey> occByIndex = new LinkedHashMap<>();
        for (Assignment a : winner.assignments) {
            pathByIndex.put(a.inputIndex, a.path);
            occByIndex.put(a.inputIndex, a.occurrence);
        }
        return RoutingResult.success(pathByIndex, occByIndex);
    }

    private static boolean assignmentsEqual(List<Assignment> a, List<Assignment> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            Assignment x = a.get(i);
            Assignment y = b.get(i);
            if (x.inputIndex != y.inputIndex
                    || !x.path.equals(y.path)
                    || !x.occurrence.equals(y.occurrence)) {
                return false;
            }
        }
        return true;
    }

    private static List<MatchResult> match(XMLContentModel node,
                                           List<RoutingInput> inputs,
                                           int inputIndex,
                                           OccurrenceStack stack) {
        int remaining = inputs.size() - inputIndex;
        int min = node.minOccursOrDefault();
        int max = node.maxOccursOrDefault(remaining);
        if (max < min) {
            return Collections.emptyList();
        }

        List<MatchResult> results = new ArrayList<>();
        // Try every legal occurrence count from min..max.
        repeat(node, inputs, inputIndex, 0, min, max, Collections.emptyList(), stack, results);
        return results;
    }

    private static void repeat(XMLContentModel node,
                               List<RoutingInput> inputs,
                               int inputIndex,
                               int count,
                               int min,
                               int max,
                               List<Assignment> accumulated,
                               OccurrenceStack stack,
                               List<MatchResult> out) {
        if (count >= min) {
            out.add(new MatchResult(inputIndex, accumulated));
        }
        if (count >= max) {
            return;
        }
        // Push a fresh occurrence frame for this iteration, but only for grouping (compound) nodes.
        // Leaf-like nodes (ELEMENT, ANY) must inherit the parent's occurrence so that two leaves
        // matched within the same enclosing repeated iteration share the same OccurrenceKey.
        OccurrenceStack pushed;
        switch (node.getNodeType()) {
            case ELEMENT:
            case ANY:
                pushed = stack;
                break;
            default:
                pushed = stack.pushOccurrence(node, count);
                break;
        }
        List<MatchResult> singles = matchOnce(node, inputs, inputIndex, pushed);
        for (MatchResult one : singles) {
            // Must consume at least one input or it would loop forever; allow zero-consumption only
            // if min<=count already (handled by the early return on count==min above), and skip here.
            if (one.nextIndex == inputIndex && one.assignments.isEmpty()) {
                continue;
            }
            List<Assignment> merged = concat(accumulated, one.assignments);
            repeat(node, inputs, one.nextIndex, count + 1, min, max, merged, stack, out);
        }
    }

    private static List<MatchResult> matchOnce(XMLContentModel node,
                                               List<RoutingInput> inputs,
                                               int inputIndex,
                                               OccurrenceStack stack) {
        switch (node.getNodeType()) {
            case ELEMENT:
                return matchElement(node, inputs, inputIndex, stack);
            case SEQUENCE:
                return matchSequence(node, inputs, inputIndex, stack);
            case CHOICE:
                return matchChoice(node, inputs, inputIndex, stack);
            case ALL:
                return matchAll(node, inputs, inputIndex, stack);
            case ANY:
                return matchAny(node, inputs, inputIndex, stack);
            default:
                throw new IllegalArgumentException("Unsupported XML content-model node type: " + node.getNodeType());
        }
    }

    private static List<MatchResult> matchElement(XMLContentModel node,
                                                  List<RoutingInput> inputs,
                                                  int inputIndex,
                                                  OccurrenceStack stack) {
        if (inputIndex >= inputs.size()) {
            return Collections.emptyList();
        }
        RoutingInput input = inputs.get(inputIndex);
        Optional<String> requiredName = node.getXmlName();
        if (!requiredName.isPresent() || !requiredName.get().equals(input.getXmlName())) {
            return Collections.emptyList();
        }
        Optional<String> requiredNs = node.getNamespace();
        if (requiredNs.isPresent()) {
            if (!input.getNamespace().isPresent() || !requiredNs.get().equals(input.getNamespace().get())) {
                return Collections.emptyList();
            }
        }
        List<Assignment> assignments;
        if (node.getPath().isPresent()) {
            assignments = Collections.singletonList(new Assignment(inputIndex, node.getPath().get(), stack.currentOccurrence()));
        } else {
            assignments = Collections.emptyList();
        }
        return Collections.singletonList(new MatchResult(inputIndex + 1, assignments));
    }

    private static List<MatchResult> matchSequence(XMLContentModel node,
                                                   List<RoutingInput> inputs,
                                                   int inputIndex,
                                                   OccurrenceStack stack) {
        List<XMLContentModel> children = node.getChildren().orElse(Collections.emptyList());
        List<MatchResult> current = Collections.singletonList(new MatchResult(inputIndex, Collections.emptyList()));
        for (XMLContentModel child : children) {
            List<MatchResult> next = new ArrayList<>();
            for (MatchResult m : current) {
                for (MatchResult c : match(child, inputs, m.nextIndex, stack)) {
                    next.add(new MatchResult(c.nextIndex, concat(m.assignments, c.assignments)));
                }
            }
            current = next;
            if (current.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return current;
    }

    private static List<MatchResult> matchChoice(XMLContentModel node,
                                                 List<RoutingInput> inputs,
                                                 int inputIndex,
                                                 OccurrenceStack stack) {
        List<XMLContentModel> children = node.getChildren().orElse(Collections.<XMLContentModel>emptyList());
        List<MatchResult> results = new ArrayList<>();
        for (XMLContentModel child : children) {
            results.addAll(match(child, inputs, inputIndex, stack));
        }
        return results;
    }

    private static List<MatchResult> matchAll(XMLContentModel node,
                                              List<RoutingInput> inputs,
                                              int inputIndex,
                                              OccurrenceStack stack) {
        // ALL: unordered set of children, each respecting its own min/max occurrence.
        List<XMLContentModel> children = node.getChildren().orElse(Collections.emptyList());
        if (children.isEmpty()) {
            return Collections.singletonList(new MatchResult(inputIndex, Collections.emptyList()));
        }
        List<MatchResult> results = new ArrayList<>();
        int[] counts = new int[children.size()];
        allRecurse(children, counts, inputs, inputIndex, Collections.emptyList(), stack, results);
        return results;
    }

    private static void allRecurse(List<XMLContentModel> children,
                                   int[] counts,
                                   List<RoutingInput> inputs,
                                   int inputIndex,
                                   List<Assignment> accumulated,
                                   OccurrenceStack stack,
                                   List<MatchResult> out) {
        // Record a current "stop" candidate iff all child min-occurrences are satisfied.
        boolean allMinSatisfied = true;
        for (int i = 0; i < children.size(); i++) {
            if (counts[i] < children.get(i).minOccursOrDefault()) {
                allMinSatisfied = false;
                break;
            }
        }
        if (allMinSatisfied) {
            out.add(new MatchResult(inputIndex, accumulated));
        }
        if (inputIndex >= inputs.size()) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            XMLContentModel child = children.get(i);
            int remaining = inputs.size() - inputIndex;
            int childMax = child.maxOccursOrDefault(remaining);
            if (counts[i] >= childMax) {
                continue;
            }
            // Single iteration of this child (without applying its own occurrence loop), because
            // ALL controls the iteration itself.
            List<MatchResult> singles = matchOnce(child, inputs, inputIndex, stack);
            for (MatchResult one : singles) {
                if (one.nextIndex == inputIndex && one.assignments.isEmpty()) {
                    continue;
                }
                counts[i]++;
                allRecurse(children, counts, inputs, one.nextIndex, concat(accumulated, one.assignments), stack, out);
                counts[i]--;
            }
        }
    }

    private static List<MatchResult> matchAny(XMLContentModel node,
                                              List<RoutingInput> inputs,
                                              int inputIndex,
                                              OccurrenceStack stack) {
        if (inputIndex >= inputs.size()) {
            return Collections.emptyList();
        }
        RoutingInput input = inputs.get(inputIndex);
        Optional<String> requiredNs = node.getNamespace();
        if (requiredNs.isPresent()) {
            if (!input.getNamespace().isPresent() || !requiredNs.get().equals(input.getNamespace().get())) {
                return Collections.emptyList();
            }
        }
        List<Assignment> assignments;
        if (node.getPath().isPresent()) {
            assignments = Collections.singletonList(new Assignment(inputIndex, node.getPath().get(), stack.currentOccurrence()));
        } else {
            assignments = Collections.emptyList();
        }
        return Collections.singletonList(new MatchResult(inputIndex + 1, assignments));
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        List<T> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    /**
     * Tracks the path of repeated content-model node iterations leading to the currently matched
     * element so that two assignments produced by the same repetition share the same
     * {@link OccurrenceKey}.
     */
    private static final class OccurrenceStack {
        private final List<OccurrenceFrame> frames;

        OccurrenceStack() {
            this.frames = Collections.emptyList();
        }

        private OccurrenceStack(List<OccurrenceFrame> frames) {
            this.frames = frames;
        }

        OccurrenceStack pushOccurrence(XMLContentModel node, int iteration) {
            List<OccurrenceFrame> next = new ArrayList<>(frames.size() + 1);
            next.addAll(frames);
            next.add(new OccurrenceFrame(System.identityHashCode(node), iteration));
            return new OccurrenceStack(next);
        }

        OccurrenceKey currentOccurrence() {
            return new OccurrenceKey(new ArrayList<>(frames));
        }
    }

    private static final class OccurrenceFrame {
        final int nodeId;
        final int iteration;

        OccurrenceFrame(int nodeId, int iteration) {
            this.nodeId = nodeId;
            this.iteration = iteration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OccurrenceFrame)) return false;
            OccurrenceFrame that = (OccurrenceFrame) o;
            return nodeId == that.nodeId && iteration == that.iteration;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, iteration);
        }
    }

    static final class OccurrenceKey {
        private final List<OccurrenceFrame> frames;

        private OccurrenceKey(List<OccurrenceFrame> frames) {
            this.frames = frames;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OccurrenceKey)) return false;
            OccurrenceKey that = (OccurrenceKey) o;
            return frames.equals(that.frames);
        }

        @Override
        public int hashCode() {
            return frames.hashCode();
        }

        @Override
        public String toString() {
            return "occ" + frames;
        }
    }

    private static final class Assignment {
        final int inputIndex;
        final List<String> path;
        final OccurrenceKey occurrence;

        Assignment(int inputIndex, List<String> path, OccurrenceKey occurrence) {
            this.inputIndex = inputIndex;
            this.path = path;
            this.occurrence = occurrence;
        }
    }

    private static final class MatchResult {
        final int nextIndex;
        final List<Assignment> assignments;

        MatchResult(int nextIndex, List<Assignment> assignments) {
            this.nextIndex = nextIndex;
            this.assignments = assignments;
        }
    }

    /**
     * Final routing decision for one parent object, exposed to the deserializer wrapper.
     */
    static final class RoutingResult {
        enum Status { SUCCESS, NO_MATCH, AMBIGUOUS }

        private final Status status;
        private final Map<Integer, List<String>> pathByIndex;
        private final Map<Integer, OccurrenceKey> occurrenceByIndex;
        private final List<List<List<String>>> candidatePaths;

        private RoutingResult(Status status,
                              Map<Integer, List<String>> pathByIndex,
                              Map<Integer, OccurrenceKey> occurrenceByIndex,
                              List<List<List<String>>> candidatePaths) {
            this.status = status;
            this.pathByIndex = pathByIndex;
            this.occurrenceByIndex = occurrenceByIndex;
            this.candidatePaths = candidatePaths;
        }

        static RoutingResult noMatch() {
            return new RoutingResult(Status.NO_MATCH, Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyList());
        }

        static RoutingResult ambiguous(List<MatchResult> candidates) {
            List<List<List<String>>> alts = new ArrayList<>();
            for (MatchResult mr : candidates) {
                List<List<String>> paths = new ArrayList<>();
                for (Assignment a : mr.assignments) {
                    paths.add(a.path);
                }
                alts.add(paths);
            }
            return new RoutingResult(Status.AMBIGUOUS, Collections.emptyMap(),
                    Collections.emptyMap(), alts);
        }

        static RoutingResult success(Map<Integer, List<String>> pathByIndex,
                                     Map<Integer, OccurrenceKey> occurrenceByIndex) {
            return new RoutingResult(Status.SUCCESS, pathByIndex, occurrenceByIndex,
                    Collections.emptyList());
        }

        Status getStatus() {
            return status;
        }

        Map<Integer, List<String>> getPathByIndex() {
            return pathByIndex;
        }

        Map<Integer, OccurrenceKey> getOccurrenceByIndex() {
            return occurrenceByIndex;
        }

        List<List<List<String>>> getCandidatePaths() {
            return candidatePaths;
        }
    }
}
