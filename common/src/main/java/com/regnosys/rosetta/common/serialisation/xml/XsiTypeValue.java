package com.regnosys.rosetta.common.serialisation.xml;

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

import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap.XMLFullyQualifiedName;

import java.util.Optional;

/**
 * The value {@link XsiTypeResolvingStreamReader} gives an element's xsi:type in the {@link #FIELD_NAME}
 * attribute it adds, and how {@code XsiTypeDeserializer} reads it back.
 *
 * <p>A value whose prefix the reader resolved is {@code {namespace}localName}, with an empty namespace for
 * a type in no namespace. Any other value, one that is not a QName or whose prefix is not declared, is
 * {@code !} followed by the text the document wrote, so it can never pass for a resolved one.
 */
public final class XsiTypeValue {
    /**
     * The name the resolved value is read under. It cannot be the name of an XML attribute or element of
     * the model, since a local name has no colon.
     */
    public static final String FIELD_NAME = "xsi:type";

    private static final char UNRESOLVED = '!';

    private XsiTypeValue() {
    }

    static String resolved(String namespace, String localName) {
        return "{" + (namespace == null ? "" : namespace) + "}" + localName;
    }

    static String unresolved(String text) {
        return UNRESOLVED + text;
    }

    /** The XSD type the value names, or empty when the reader could not resolve it. */
    public static Optional<XMLFullyQualifiedName> typeName(String value) {
        if (value == null || value.isEmpty() || value.charAt(0) != '{') {
            return Optional.empty();
        }
        // A local name cannot contain '}', but a namespace can.
        int close = value.lastIndexOf('}');
        if (close < 1 || close == value.length() - 1) {
            return Optional.empty();
        }
        String namespace = value.substring(1, close);
        return Optional.of(new XMLFullyQualifiedName(value.substring(close + 1), namespace.isEmpty() ? null : namespace));
    }

    /**
     * The value as it can be shown in a log message: the text the document wrote when it was not resolved,
     * with control characters escaped, since the text comes from the document.
     */
    public static String describe(String value) {
        String text = value != null && !value.isEmpty() && value.charAt(0) == UNRESOLVED ? value.substring(1) : value;
        if (text == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c)) {
                escaped.append(String.format("\\u%04x", (int) c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
