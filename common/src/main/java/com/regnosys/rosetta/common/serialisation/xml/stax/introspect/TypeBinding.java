package com.regnosys.rosetta.common.serialisation.xml.stax.introspect;

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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The complete XML binding plan for one Rune-generated type.
 * Produced by {@link RuneTypeIntrospector}; consumed by the StAX writer and reader.
 */
public class TypeBinding {

    private final Class<?> type;
    private final Class<?> builderType;
    private final List<AttributeBinding> attributes;
    private final String xmlElementName;
    private final Optional<String> xmlElementNamespace;
    private final Map<String, String> xmlConstantAttributes;
    private final Optional<XMLContentModel> contentModel;
    private final boolean isAbstract;

    public TypeBinding(
            Class<?> type,
            Class<?> builderType,
            List<AttributeBinding> attributes,
            String xmlElementName,
            Optional<String> xmlElementNamespace,
            Map<String, String> xmlConstantAttributes,
            Optional<XMLContentModel> contentModel,
            boolean isAbstract) {
        this.type = type;
        this.builderType = builderType;
        this.attributes = Collections.unmodifiableList(attributes);
        this.xmlElementName = xmlElementName;
        this.xmlElementNamespace = xmlElementNamespace;
        this.xmlConstantAttributes = Collections.unmodifiableMap(xmlConstantAttributes);
        this.contentModel = contentModel;
        this.isAbstract = isAbstract;
    }

    /** The Rune interface type (e.g., {@code Party.class}). */
    public Class<?> getType() {
        return type;
    }

    /** The generated builder impl class from {@code @RuneDataType.builder()}. */
    public Class<?> getBuilderType() {
        return builderType;
    }

    /**
     * Attributes in bean declaration order: supertype attributes come before
     * subtype attributes, matching the order of the generated source file.
     */
    public List<AttributeBinding> getAttributes() {
        return attributes;
    }

    /** XML element local name for this type (root or child), resolved from config or defaulting to the type's logical name. */
    public String getXmlElementName() {
        return xmlElementName;
    }

    /**
     * XML namespace URI for this type's element, derived from the {@code xmlElementFullyQualifiedName}
     * config field (the portion before the last {@code /}).
     */
    public Optional<String> getXmlElementNamespace() {
        return xmlElementNamespace;
    }

    /**
     * Constant XML attributes to emit on this element (e.g., {@code xmlns:} prefix declarations,
     * {@code xsi:schemaLocation}), from the {@code xmlAttributes} config field.
     */
    public Map<String, String> getXmlConstantAttributes() {
        return xmlConstantAttributes;
    }

    /**
     * Content-model descriptor for disambiguation.
     * Present only for the ~2 types per config that carry {@code contentModel} (ambiguous types).
     */
    public Optional<XMLContentModel> getContentModel() {
        return contentModel;
    }

    /** {@code true} when the XSD type is abstract (no concrete element can use this type directly). */
    public boolean isAbstract() {
        return isAbstract;
    }
}
