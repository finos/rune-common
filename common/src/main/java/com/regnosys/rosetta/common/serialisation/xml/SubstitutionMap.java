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
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An map defining the relation between types and
 * their substituted name. This is a representation
 * of all XML substitution groups defined in an XSD schema.
 */
public class SubstitutionMap {
    private final Map<JavaType, XMLFullyQualifiedName> typeToFullyQualifiedNameMap;
    private final Map<XMLFullyQualifiedName, JavaType> fullyQualifiedNameToTypeMap;
    private final Multimap<String, JavaType> localNameToTypeMap;

    /**
     * Holds the XML element name and optional namespace for a type.
     */
    public static class XMLFullyQualifiedName {
        private final String name;
        private final String namespace;

        public XMLFullyQualifiedName(String name) {
            Matcher namespaceAndLocalNameMatcher = getNamespaceAndLocalNameMatcher(name);
            this.name = generateLocalName(name, namespaceAndLocalNameMatcher);
            this.namespace = generateNamespace(namespaceAndLocalNameMatcher);
        }

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

        private static String generateNamespace(Matcher namespaceAndLocalNameMatcher) {
            if (namespaceAndLocalNameMatcher.matches()) {
                return namespaceAndLocalNameMatcher.group(1);
            }
            return null;
        }

        private static String generateLocalName(String name, Matcher namespaceAndLocalNameMatcher) {
            if (namespaceAndLocalNameMatcher.matches()) {
                return namespaceAndLocalNameMatcher.group(2);
            }
            return name;
        }

        private static Matcher getNamespaceAndLocalNameMatcher(String name) {
            Pattern p = Pattern.compile("^(.+)/(.+)$");
            return p.matcher(name);
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

        @Override
        public String toString() {
            return namespace != null ? namespace + "/" + name : name;
        }
    }

    public SubstitutionMap(Map<JavaType, XMLFullyQualifiedName> typeToFullyQualifiedNameMap, Map<XMLFullyQualifiedName, JavaType> fullyQualifiedNameToTypeMap) {
        this.typeToFullyQualifiedNameMap = new LinkedHashMap<>();
        this.localNameToTypeMap = MultimapBuilder.hashKeys().arrayListValues().build();
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
        }).forEach(key -> {
            XMLFullyQualifiedName fqn = typeToFullyQualifiedNameMap.get(key);
            this.typeToFullyQualifiedNameMap.put(key, fqn);
            if (fqn != null && fqn.getName() != null) {
                localNameToTypeMap.put(fqn.getName(), key);
            }
        });
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
        XMLFullyQualifiedName fullyQualifiedName = typeToFullyQualifiedNameMap.get(type);
        return fullyQualifiedName != null ? fullyQualifiedName.getName() : null;
    }

    public String getNamespace(JavaType type) {
        XMLFullyQualifiedName fullyQualifiedName = typeToFullyQualifiedNameMap.get(type);
        return fullyQualifiedName != null ? fullyQualifiedName.getNamespace() : null;
    }

    public JavaType getTypeByFullyQualifiedName(XMLFullyQualifiedName fullyQualifiedName) {
        return fullyQualifiedNameToTypeMap.get(fullyQualifiedName);
    }

    public Collection<JavaType> getTypesByLocalName(String localName) {
        return localNameToTypeMap.get(localName);
    }
}
