package com.regnosys.rosetta.common.serialisation.xml.stax.write;

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

import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.rosetta.test.Foo;
import com.rosetta.test.MulticardinalityContainer;
import com.rosetta.test.Party;
import com.rosetta.test.PartyModel;
import com.rosetta.test.PartyNameModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StaxWriter} covering VIRTUAL attribute serialisation (Step 3c).
 *
 * <p>VIRTUAL attributes are transparent wrappers: their children are written directly
 * into the parent element with no wrapper element emitted.
 *
 * <p>Also covers multi-cardinality serialisation not present in {@link StaxWriterTest}.
 */
public class StaxWriterVirtualTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/extension-schema-xml-config.json";

    private StaxWriter writer;
    private String licenseHeader;

    @BeforeEach
    public void setUp() throws Exception {
        URL configUrl = Resources.getResource(XML_CONFIG);
        RosettaXMLConfiguration config;
        try (InputStream is = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(is);
        }
        writer = new StaxWriter(config);
        licenseHeader = Resources.toString(
                Resources.getResource("serialisation/xml/expected/license-header.xml"),
                StandardCharsets.UTF_8);
    }

    @Test
    public void testVirtualAttributes() throws Exception {
        Party party = Party.builder()
                .setPartyNameModel(PartyNameModel.builder().setPartyName("my name"))
                .setPartyModel(PartyModel.builder().addPartyId("myId1").addPartyId("myId2"))
                .build();

        String actual = licenseHeader + writer.write(party, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/virtual-attributes.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testMultiCardinalitySerialisation() throws Exception {
        MulticardinalityContainer mc = MulticardinalityContainer.builder()
                .addFoo(Foo.builder().setXmlValue("foo1").addAttr1("Foo").addAttr1("Bar").build())
                .addFoo(Foo.builder().setXmlValue("foo2").addAttr1("Qux").build())
                .build();

        String actual = licenseHeader + writer.write(mc, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/multicardinality-container.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }
}
