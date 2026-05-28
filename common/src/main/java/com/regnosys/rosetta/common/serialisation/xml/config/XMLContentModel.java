package com.regnosys.rosetta.common.serialisation.xml.config;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable description of an XML content-model node used by the disambiguating XML deserializer
 * to route ambiguous XML element names to the correct Rosetta property path.
 *
 * <p>Each node has a {@link XMLContentModelNodeType}. For {@code ELEMENT} (and {@code ANY} with a
 * routing path) nodes, {@link #getPath()} identifies the logical Rosetta property path. Nested
 * child nodes are represented by {@link #getChildren()}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public final class XMLContentModel {

    private final XMLContentModelNodeType nodeType;
    private final Optional<String> xmlName;
    private final Optional<String> namespace;
    private final Optional<List<String>> path;
    private final Optional<Integer> minOccurs;
    private final Optional<OccursMax> maxOccurs;
    private final Optional<List<XMLContentModel>> children;

    @JsonCreator
    public XMLContentModel(
            @JsonProperty("nodeType") XMLContentModelNodeType nodeType,
            @JsonProperty("xmlName") Optional<String> xmlName,
            @JsonProperty("namespace") Optional<String> namespace,
            @JsonProperty("path") Optional<List<String>> path,
            @JsonProperty("minOccurs") Optional<Integer> minOccurs,
            @JsonProperty("maxOccurs") Optional<OccursMax> maxOccurs,
            @JsonProperty("children") Optional<List<XMLContentModel>> children) {
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType");
        this.xmlName = xmlName;
        this.namespace = namespace;
        this.path = path.map(values -> Collections.unmodifiableList(new ArrayList<>(values)));
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.children = children.map(values -> Collections.unmodifiableList(new ArrayList<>(values)));
    }

    public XMLContentModelNodeType getNodeType() {
        return nodeType;
    }

    public Optional<String> getXmlName() {
        return xmlName;
    }

    public Optional<String> getNamespace() {
        return namespace;
    }

    public Optional<List<String>> getPath() {
        return path;
    }

    public Optional<Integer> getMinOccurs() {
        return minOccurs;
    }

    public Optional<OccursMax> getMaxOccurs() {
        return maxOccurs;
    }

    public Optional<List<XMLContentModel>> getChildren() {
        return children;
    }

    /**
     * @return effective {@code minOccurs}, defaulting to {@code 1}.
     */
    public int minOccursOrDefault() {
        return minOccurs.orElse(1);
    }

    /**
     * @return effective {@code maxOccurs} bounded by the supplied input remaining (used for
     *         "unbounded"); defaults to {@code 1}.
     */
    public int maxOccursOrDefault(int inputRemaining) {
        return maxOccurs.map(occursMax -> occursMax.boundedValue(inputRemaining)).orElse(1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof XMLContentModel)) {
            return false;
        }
        XMLContentModel other = (XMLContentModel) obj;
        return nodeType == other.nodeType
                && Objects.equals(xmlName, other.xmlName)
                && Objects.equals(namespace, other.namespace)
                && Objects.equals(path, other.path)
                && Objects.equals(minOccurs, other.minOccurs)
                && Objects.equals(maxOccurs, other.maxOccurs)
                && Objects.equals(children, other.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeType, xmlName, namespace, path, minOccurs, maxOccurs, children);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nodeType);
        xmlName.ifPresent(n -> sb.append("[").append(n).append("]"));
        path.ifPresent(p -> sb.append("->").append(p));
        return sb.toString();
    }
}
