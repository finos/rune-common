package com.regnosys.rosetta.common.serialisation.xml.serialization;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes the order in which a Rosetta type's child elements should be serialised so that the
 * resulting XML follows the configured XSD content model. This is the serialisation-time dual of
 * {@link com.regnosys.rosetta.common.serialisation.xml.deserialization.XMLContentModelMatcher}:
 * the matcher routes an <em>ordered</em> input against the content model, whereas this orderer
 * takes the <em>set of present properties</em> and produces a valid ordering against the same model.
 *
 * <p>Ordering is computed at the granularity of Rosetta properties (the first segment of each
 * content-model element path). This is sound because every property maps to a single contiguous
 * block of XML elements: a direct element is one block, and a {@code VIRTUAL} group's leaves are a
 * contiguous subtree of the content model. Working at property granularity lets the orderer pick a
 * data-dependent branch order (e.g. {@code x,a,b} vs {@code y,b,a}) that a static property
 * reordering could never express.</p>
 *
 * <p>When the present set cannot be fully consumed by the model (an unexpected combination, or a
 * model shape the orderer does not handle), {@link #order(Set)} returns {@code null} and the caller
 * falls back to the default serialisation order. The change is therefore never worse than today.</p>
 */
final class XMLContentModelOrderer {

    /** Safety bound on the search to avoid pathological blow-ups; falls back to default order. */
    private static final int MAX_CANDIDATES = 256;

    private final Node root;
    private final Set<String> contentModelProperties;

    XMLContentModelOrderer(XMLContentModel contentModel) {
        this.contentModelProperties = Collections.unmodifiableSet(leafProperties(contentModel, new LinkedHashSet<>()));
        this.root = reduce(contentModel);
    }

    /**
     * @return the set of Rosetta property names that participate in this content model (the first
     *         path segment of each element). Only these properties are reordered; all others keep
     *         their position.
     */
    Set<String> getContentModelProperties() {
        return contentModelProperties;
    }

    /**
     * Compute a valid serialisation order for the supplied present properties.
     *
     * @param presentProperties the content-model properties that actually have a value on the bean.
     * @return an ordering consuming exactly {@code presentProperties}, or {@code null} if no such
     *         ordering could be derived (caller should keep the default order).
     */
    List<String> order(Set<String> presentProperties) {
        if (presentProperties.isEmpty()) {
            return Collections.emptyList();
        }
        if (root == null) {
            return null;
        }
        List<List<String>> candidates = new ArrayList<>();
        enumerate(root, presentProperties, candidates);
        for (List<String> candidate : candidates) {
            if (candidate.size() == presentProperties.size()
                    && new LinkedHashSet<>(candidate).equals(presentProperties)) {
                return candidate;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Reduction: collapse each single-property subtree into one PROP node.
    // -------------------------------------------------------------------------

    private static Node reduce(XMLContentModel model) {
        Set<String> props = leafProperties(model, new LinkedHashSet<>());
        boolean required = model.minOccursOrDefault() >= 1;
        if (props.size() <= 1) {
            if (props.isEmpty()) {
                return null; // contributes no element (e.g. ANY without a routing path)
            }
            return new Node(props.iterator().next(), required);
        }
        List<Node> children = new ArrayList<>();
        model.getChildren().ifPresent(cs -> {
            for (XMLContentModel c : cs) {
                Node reduced = reduce(c);
                if (reduced != null) {
                    children.add(reduced);
                }
            }
        });
        return new Node(model.getNodeType(), children, required);
    }

    private static Set<String> leafProperties(XMLContentModel node, Set<String> acc) {
        XMLContentModelNodeType type = node.getNodeType();
        if (type == XMLContentModelNodeType.ELEMENT || type == XMLContentModelNodeType.ANY) {
            node.getPath().filter(p -> !p.isEmpty()).ifPresent(p -> acc.add(p.get(0)));
        }
        node.getChildren().ifPresent(cs -> {
            for (XMLContentModel c : cs) {
                leafProperties(c, acc);
            }
        });
        return acc;
    }

    // -------------------------------------------------------------------------
    // Enumeration of valid property orderings.
    // -------------------------------------------------------------------------

    private void enumerate(Node node, Set<String> present, List<List<String>> out) {
        if (node.property != null) {
            if (present.contains(node.property)) {
                out.add(new ArrayList<>(Collections.singletonList(node.property)));
            } else if (!node.required) {
                out.add(new ArrayList<>());
            }
            // required but absent -> no result (this branch cannot be satisfied)
            return;
        }
        switch (node.nodeType) {
            case CHOICE:
                enumerateChoice(node, present, out);
                break;
            case SEQUENCE:
            case ALL:
            default:
                enumerateSequence(node, present, out);
                break;
        }
    }

    private void enumerateChoice(Node node, Set<String> present, List<List<String>> out) {
        for (Node child : node.children) {
            enumerate(child, present, out);
            if (out.size() > MAX_CANDIDATES) {
                return;
            }
        }
        if (out.isEmpty() && !node.required) {
            out.add(new ArrayList<>());
        }
    }

    private void enumerateSequence(Node node, Set<String> present, List<List<String>> out) {
        List<List<String>> acc = new ArrayList<>();
        acc.add(new ArrayList<>());
        for (Node child : node.children) {
            List<List<String>> childOptions = new ArrayList<>();
            enumerate(child, present, childOptions);
            if (childOptions.isEmpty()) {
                return; // a required child could not be satisfied -> whole sequence fails
            }
            List<List<String>> next = new ArrayList<>();
            for (List<String> prefix : acc) {
                for (List<String> suffix : childOptions) {
                    if (disjoint(prefix, suffix)) {
                        List<String> combined = new ArrayList<>(prefix);
                        combined.addAll(suffix);
                        next.add(combined);
                        if (next.size() > MAX_CANDIDATES) {
                            out.addAll(next);
                            return;
                        }
                    }
                }
            }
            acc = next;
        }
        out.addAll(acc);
    }

    private static boolean disjoint(List<String> a, List<String> b) {
        for (String s : b) {
            if (a.contains(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A reduced content-model node: either a single property ({@link #property} != null) or a
     * compound group with a {@link #nodeType} and {@link #children}.
     */
    private static final class Node {
        final String property;
        final XMLContentModelNodeType nodeType;
        final List<Node> children;
        final boolean required;

        Node(String property, boolean required) {
            this.property = property;
            this.nodeType = null;
            this.children = Collections.emptyList();
            this.required = required;
        }

        Node(XMLContentModelNodeType nodeType, List<Node> children, boolean required) {
            this.property = null;
            this.nodeType = nodeType;
            this.children = children;
            this.required = required;
        }
    }
}
