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
import com.rosetta.test.Measure;
import com.rosetta.test.TimeContainer;
import com.rosetta.test.TopLevel;
import com.rosetta.test.TopLevelExtension;
import com.rosetta.test.TypeWithTypeElement;
import com.rosetta.test.UnitEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StaxWriter} covering the main XML serialisation scenarios.
 *
 * <p>VIRTUAL attributes (e.g. Party) are out of scope for Step 3a and are not tested here.
 */
public class StaxWriterTest {

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
    public void testDocumentSerialisation() throws Exception {
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        TopLevel document = TopLevel.builder().setAttr(foo).setValue(measure).build();

        Map<String, String> extraRootAttrs = new LinkedHashMap<String, String>();
        extraRootAttrs.put("xsi:schemaLocation", "urn:my.schema ../schema/schema.xsd");

        String actual = licenseHeader + writer.write(document, true, extraRootAttrs);
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/document.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testTopLevelExtensionSerialisation() throws Exception {
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        TopLevelExtension document = TopLevelExtension.builder()
                .setAttr(foo)
                .setValue(measure)
                .setDocumentExtensionAttr("Document Extension Attribute Value")
                .build();

        Map<String, String> extraRootAttrs = new LinkedHashMap<String, String>();
        extraRootAttrs.put("xsi:schemaLocation", "urn:my.schema ../schema/schema.xsd");

        String actual = licenseHeader + writer.write(document, true, extraRootAttrs);
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/extended-top-level-document.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testElementNamedTypeSerialisation() throws Exception {
        TypeWithTypeElement t = TypeWithTypeElement.builder()
                .setFirstElement("first")
                .setType("My type")
                .build();

        String actual = licenseHeader + writer.write(t, true, Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/element-named-type.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeSerialisation() throws Exception {
        TimeContainer timeContainer = TimeContainer.builder()
                .setTimeValue(LocalTime.of(1, 23, 45))
                .build();

        String actual = writer.write(timeContainer, false, Collections.<String, String>emptyMap());
        String expected = "<TimeContainer><timeValue>01:23:45Z</timeValue></TimeContainer>";
        assertEquals(expected, actual);
    }
}
