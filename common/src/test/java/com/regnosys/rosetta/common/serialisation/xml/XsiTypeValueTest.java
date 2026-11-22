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
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XsiTypeValueTest {

    @Test
    void resolvedValueNamesItsType() {
        assertEquals(Optional.of(new XMLFullyQualifiedName("Circle", "urn:my.schema")),
                XsiTypeValue.typeName(XsiTypeValue.resolved("urn:my.schema", "Circle")));
    }

    @Test
    void resolvedValueWithoutNamespaceNamesATypeInNoNamespace() {
        assertEquals(Optional.of(new XMLFullyQualifiedName("Circle", null)),
                XsiTypeValue.typeName(XsiTypeValue.resolved(null, "Circle")));
    }

    @Test
    void namespaceMayContainABrace() {
        assertEquals(Optional.of(new XMLFullyQualifiedName("Circle", "urn:a}b")),
                XsiTypeValue.typeName(XsiTypeValue.resolved("urn:a}b", "Circle")));
    }

    @Test
    void unresolvedValueNamesNoType() {
        assertEquals(Optional.empty(), XsiTypeValue.typeName(XsiTypeValue.unresolved("{urn:my.schema}Circle")));
    }

    @Test
    void valueNotWrittenByTheReaderNamesNoType() {
        assertEquals(Optional.empty(), XsiTypeValue.typeName("{abc"));
        assertEquals(Optional.empty(), XsiTypeValue.typeName("{urn:my.schema}"));
        assertEquals(Optional.empty(), XsiTypeValue.typeName("Circle"));
        assertEquals(Optional.empty(), XsiTypeValue.typeName(""));
    }

    @Test
    void describeGivesTheTextWithControlCharactersEscaped() {
        assertEquals("un:Tri\\u0001angle", XsiTypeValue.describe(XsiTypeValue.unresolved("un:Tri\u0001angle")));
        assertEquals("{urn:my.schema}Circle", XsiTypeValue.describe(XsiTypeValue.resolved("urn:my.schema", "Circle")));
    }
}
