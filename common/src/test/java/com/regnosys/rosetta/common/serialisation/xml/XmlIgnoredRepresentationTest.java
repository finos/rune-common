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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.test.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XmlIgnoredRepresentationTest {
    private static final String CONFIG_PATH = "xml-serialisation/xml-config/ignored-representation-xml-config.json";
    
    private ObjectMapper xmlMapper;
    
    @BeforeAll
    void setup() throws IOException {
        URL configUrl = Resources.getResource(CONFIG_PATH);
        try (InputStream inputStream = configUrl.openStream()) {
            xmlMapper = RosettaObjectMapperCreator.forXML(inputStream).create();
        }
    }
    
    @Test
    void testIgnoredPropertyIsNotDeserialized() throws IOException {
        String xml = "<Animal><name>Ignored</name></Animal>";

        Animal actual = xmlMapper.readValue(xml, Animal.class);

        assertEquals(Animal.builder().build(), actual);
        assertNull(actual.getName());
    }

    @Test
    void testIgnoredUnwrappedPropertyUsesNonIgnored() throws IOException {
        String xml = "<NestedContainer><shared>10</shared><d>20</d></NestedContainer>";

        NestedContainer actual = xmlMapper.readValue(xml, NestedContainer.class);

        NestedContainer expected = NestedContainer.builder()
                .setNestedContainerSequence0(NestedContainerSequence0.builder().build())
                .addNestedContainerSequence1(NestedContainerSequence1.builder().setC(10).setD(20).build())
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void testIgnoredFlatPropertyPrefersUnwrappedList() throws IOException {
        String xml = "<PartyWithIgnoredFlatPartyId><partyId>one</partyId><partyId>two</partyId></PartyWithIgnoredFlatPartyId>";

        PartyWithIgnoredFlatPartyId actual = xmlMapper.readValue(xml, PartyWithIgnoredFlatPartyId.class);

        PartyWithIgnoredFlatPartyId expected = PartyWithIgnoredFlatPartyId.builder()
                .setPartyModel(PartyModel.builder()
                        .addPartyId("one")
                        .addPartyId("two"))
                .build();

        assertEquals(expected, actual);
        assertNull(actual.getPartyId());
    }
}
