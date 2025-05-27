package org.finos.rune.mapper.introspector;

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

import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.rosetta.model.lib.annotations.RuneAttribute;

import java.lang.reflect.Method;

public class RuneBeanUtil {

    public static String getPropertyName(AnnotatedMethod method) {
        RuneAttribute attribute = method.getAnnotation(RuneAttribute.class);
        if (attribute != null && !attribute.value().isEmpty()) {
            return attribute.value();
        }
        String methodName = method.getName();
        String rawPropertyName = getSubstringIfPrefixMatches(methodName, "get");
        if (rawPropertyName == null) {
            rawPropertyName = getSubstringIfPrefixMatches(methodName, "set");
        }

        if (rawPropertyName == null) {
            rawPropertyName = getSubstringIfPrefixMatches(methodName, "is");
        }

        if (rawPropertyName == null) {
            rawPropertyName = getSubstringIfPrefixMatches(methodName, "add");
        }

        return toLowerCamelCase(rawPropertyName);
    }

    public static String toLowerCamelCase(String string) {
        if (string == null) {
            return null;
        } else if (string.isEmpty()) {
            return string;
        } else if (string.length() > 1 && Character.isUpperCase(string.charAt(1)) && Character.isUpperCase(string.charAt(0))) {
            return string;
        } else {
            char[] chars = string.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            return new String(chars);
        }
    }

    private static String getSubstringIfPrefixMatches(String wholeString, String prefix) {
        return wholeString.startsWith(prefix) ? wholeString.substring(prefix.length()) : null;
    }
}
