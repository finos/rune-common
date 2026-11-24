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

import com.fasterxml.jackson.core.JsonToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the jackson-dataformat-xml behaviour {@link RosettaXmlParser#readerIsOnStartedElement()} relies on,
 * so that a Jackson version that changes it fails here rather than silently dropping xsi:type.
 */
class RosettaXmlParserTest {

    @Test
    void readerIsOnTheRootElementWithOrWithoutAttributes() throws IOException {
        assertEquals(mapOf("root", true), startedElements("<root a=\"1\"><x>1</x></root>"));
        assertEquals(mapOf("root", true), startedElements("<root><x>1</x></root>"));
    }

    @Test
    void readerIsOnANestedElementOnlyWhenItHasAttributes() throws IOException {
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("root", true);
        expected.put("withAttributes", true);
        expected.put("withoutAttributes", false);
        assertEquals(expected, startedElements(
                "<root><withAttributes a=\"1\"><x>1</x></withAttributes><withoutAttributes><x>1</x></withoutAttributes></root>"));
    }

    /** For each element read as an object, in document order: its name, and whether the reader was on it. */
    private static Map<String, Boolean> startedElements(String xml) throws IOException {
        Map<String, Boolean> result = new LinkedHashMap<>();
        try (RosettaXmlParser parser = (RosettaXmlParser) new RosettaXmlFactory().createParser(xml)) {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.START_OBJECT) {
                    result.put(parser.startedElementName(), parser.readerIsOnStartedElement());
                }
            }
        }
        return result;
    }

    private static Map<String, Boolean> mapOf(String name, boolean readerIsOnIt) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(name, readerIsOnIt);
        return map;
    }
}
