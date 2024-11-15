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

/**
 * An map defining the relation between types and
 * their substituted name. This is a representation
 * of all XML substitution groups defined in an XSD schema.
 */
public class SubstitutionMap {
    private final Map<JavaType, String> typeToNameMap;

    public SubstitutionMap(Map<JavaType, String> typeToNameMap) {
        this.typeToNameMap = new LinkedHashMap<>();
        // Sort types so subtypes come first, supertypes come later.
        typeToNameMap.keySet().stream().sorted((o1, o2) -> {
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
        }).forEach(key -> this.typeToNameMap.put(key, typeToNameMap.get(key)));
    }

    public String getSubstitutedName(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        return typeToNameMap.entrySet().stream()
                .filter(e -> e.getKey().isTypeOrSuperTypeOf(clazz))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public Collection<JavaType> getTypes() {
        return typeToNameMap.keySet();
    }
    public String getName(JavaType type) {
        return typeToNameMap.get(type);
    }
}
