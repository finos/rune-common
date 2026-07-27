package com.regnosys.rosetta.common.serialisation.xml.stax;

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
import com.rosetta.test.Measure;
import com.rosetta.test.TopLevel;
import com.rosetta.test.UnitEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link RuneXmlMapper} directly, i.e. the Jackson-free native API for new consumers of
 * the StAX binder, as opposed to the {@code StaxXmlObjectMapper} facade that
 * {@code RosettaObjectMapperCreator.forXML(...)} returns for existing callers (covered by
 * {@code XmlSerialisationTest} et al.).
 */
public class RuneXmlMapperTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/extension-schema-xml-config.json";

    private RuneXmlMapper mapper;
    private String licenseHeader;

    @BeforeEach
    public void setUp() throws Exception {
        URL configUrl = Resources.getResource(XML_CONFIG);
        RosettaXMLConfiguration config;
        try (InputStream is = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(is);
        }
        mapper = new RuneXmlMapper(config);
        licenseHeader = Resources.toString(
                Resources.getResource("serialisation/xml/expected/license-header.xml"),
                StandardCharsets.UTF_8);
    }

    private static TopLevel newDocument() {
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        return TopLevel.builder().setAttr(foo).setValue(measure).build();
    }

    @Test
    public void writeThenReadRoundTrips() throws Exception {
        TopLevel document = newDocument();

        String xml = mapper.writeValueAsString(document);
        TopLevel actual = mapper.readValue(xml, TopLevel.class);

        assertEquals(document, actual);
    }

    @Test
    public void readValueFromReaderRoundTrips() throws Exception {
        TopLevel document = newDocument();
        String xml = mapper.writeValueAsString(document);

        TopLevel actual = mapper.readValue(new StringReader(xml), TopLevel.class);

        assertEquals(document, actual);
    }

    @Test
    public void writerWithDefaultPrettyPrinterAndSchemaLocationMatchesFacadeOutput() throws Exception {
        TopLevel document = newDocument();

        String actualXML = licenseHeader + mapper.writerWithDefaultPrettyPrinter()
                .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd")
                .writeValueAsString(document);

        String expectedXML = Resources.toString(
                Resources.getResource("serialisation/xml/expected/document.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);
    }
}
