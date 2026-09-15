package com.regnosys.rosetta.common.util;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LineEndingsTest {

    @Test
    void crlfIsNormalisedToLf() {
        assertEquals("a\nb\nc", LineEndings.normalise("a\r\nb\r\nc"));
    }

    @Test
    void loneCarriageReturnIsNormalisedToLf() {
        assertEquals("a\nb", LineEndings.normalise("a\rb"));
    }

    @Test
    void lfIsLeftUnchanged() {
        assertEquals("a\nb\nc", LineEndings.normalise("a\nb\nc"));
    }

    @Test
    void contentWithoutLineBreaksIsLeftUnchanged() {
        assertEquals("abc", LineEndings.normalise("abc"));
    }

    @Test
    void nullIsReturnedForNull() {
        assertNull(LineEndings.normalise(null));
    }

    @Test
    void aCrlfDocumentAndAnLfDocumentNormaliseToTheSameValue() {
        String lf = "{\n  \"a\" : 1\n}";
        String crlf = "{\r\n  \"a\" : 1\r\n}";
        assertEquals(LineEndings.normalise(lf), LineEndings.normalise(crlf));
    }
}
