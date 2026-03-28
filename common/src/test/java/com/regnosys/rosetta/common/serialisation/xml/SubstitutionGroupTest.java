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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import org.junit.jupiter.api.Test;
import test.substitution.Root;

import java.io.IOException;
import java.io.InputStream;

public class SubstitutionGroupTest {
    @Test
    void testSubstitutedElementOverlapsWithExistingElement() throws JsonProcessingException, IOException {
        try (InputStream configStream = getClass().getResourceAsStream("/rosetta/xml-serialisation/substitution-group/xml-config.json")) {
            ObjectMapper mapper = RosettaObjectMapperCreator.forXML(configStream).create();
            Root root = mapper.readValue("<root><foo/></root>", Root.class);
        }
    }
}
