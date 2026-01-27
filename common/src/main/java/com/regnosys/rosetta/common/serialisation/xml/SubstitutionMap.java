package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import com.fasterxml.jackson.databind.JavaType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An map defining the relation between types and
 * their substituted name. This is a representation
 * of all XML substitution groups defined in an XSD schema.
 */
public class SubstitutionMap {
    private final Map<JavaType, XMLFullyQualifiedName> typeToFullyQualifiedNameMap;
    private final Map<String, JavaType> fullyQualifiedNameToTypeMap;

    /**
     * Holds the XML element name and optional namespace for a type.
     */
    public static class XMLFullyQualifiedName {
        private final String name;
        private final String namespace;

        public XMLFullyQualifiedName(String name, String namespace) {
            this.name = name;
            this.namespace = namespace;
        }

        public String getName() {
            return name;
        }

        public String getNamespace() {
            return namespace;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            XMLFullyQualifiedName that = (XMLFullyQualifiedName) o;
            return Objects.equals(name, that.name) && Objects.equals(namespace, that.namespace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, namespace);
        }
    }

    public SubstitutionMap(Map<JavaType, XMLFullyQualifiedName> typeToFullyQualifiedNameMap, Map<String, JavaType> fullyQualifiedNameToTypeMap) {
        this.typeToFullyQualifiedNameMap = new LinkedHashMap<>();
        // Sort types so subtypes come first, supertypes come later.
        typeToFullyQualifiedNameMap.keySet().stream().sorted((o1, o2) -> {
            if (o1.equals(o2)) {
                return 0;
            }
            if (o1.getRawClass().equals(o2.getRawClass())) {
                return o1.toString().compareTo(o2.toString());
            }
            if (o1.getRawClass().isAssignableFrom(o2.getRawClass())) {
                return 1;
            }
            if (o2.getRawClass().isAssignableFrom(o1.getRawClass())) {
                return -1;
            }
            return o1.toString().compareTo(o2.toString());
        }).forEach(key -> this.typeToFullyQualifiedNameMap.put(key, typeToFullyQualifiedNameMap.get(key)));
        this.fullyQualifiedNameToTypeMap = new LinkedHashMap<>(fullyQualifiedNameToTypeMap);
    }

    public String getSubstitutedName(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        return typeToFullyQualifiedNameMap.entrySet().stream()
                .filter(e -> e.getKey().isTypeOrSuperTypeOf(clazz))
                .map(e -> e.getValue().getName())
                .findFirst()
                .orElse(null);
    }

    public Collection<JavaType> getTypes() {
        return typeToFullyQualifiedNameMap.keySet();
    }

    public String getName(JavaType type) {
        XMLFullyQualifiedName info = typeToFullyQualifiedNameMap.get(type);
        return info != null ? info.getName() : null;
    }

    public String getNamespace(JavaType type) {
        XMLFullyQualifiedName fullyQualifiedName = typeToFullyQualifiedNameMap.get(type);
        return fullyQualifiedName != null ? fullyQualifiedName.getNamespace() : null;
    }

    public JavaType getTypeByFullyQualifiedName(String fullyQualifiedName) {
        return fullyQualifiedNameToTypeMap.get(fullyQualifiedName);
    }
}
