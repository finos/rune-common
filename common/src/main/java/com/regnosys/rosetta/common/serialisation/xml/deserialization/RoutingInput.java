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

import java.util.Optional;

/**
 * One ambiguous XML child element supplied to {@link XMLContentModelMatcher#route}.
 *
 * <p>{@link #fieldIndex} preserves the original ordinal index in the parent buffered field list so
 * that the matcher's assignment can be applied back to the original token stream regardless of
 * which fields were filtered out as XML attributes or pass-through.</p>
 */
final class RoutingInput {

    private final int fieldIndex;
    private final String xmlName;
    private final Optional<String> namespace;

    RoutingInput(int fieldIndex, String xmlName, Optional<String> namespace) {
        this.fieldIndex = fieldIndex;
        this.xmlName = xmlName;
        this.namespace = namespace;
    }

    int getFieldIndex() {
        return fieldIndex;
    }

    String getXmlName() {
        return xmlName;
    }

    Optional<String> getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return xmlName + "@" + fieldIndex;
    }
}
