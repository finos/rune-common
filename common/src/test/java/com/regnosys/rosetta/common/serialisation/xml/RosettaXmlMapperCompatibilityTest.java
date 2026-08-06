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
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.rosetta.test.Foo;
import com.rosetta.test.Measure;
import com.rosetta.test.TopLevel;
import com.rosetta.test.UnitEnum;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Pins the backwards-compatibility contract of the deprecated {@link RosettaXmlMapper} alias.
 *
 * <p>The Jackson XML engine that {@code RosettaXmlMapper} used to wrap has been deleted. The name is
 * retained at its original fully-qualified location so existing imports and constructor calls keep
 * compiling; this test is what makes that a guarantee rather than an assumption. The constructor
 * signature exercised here — {@code (RosettaXMLConfiguration, ClassLoader)} — is byte-for-byte the
 * one public constructor the old class exposed.</p>
 *
 * <p>Note what is deliberately <em>not</em> asserted: assignability to Jackson's {@code XmlMapper}.
 * That supertype cannot be restored because {@code jackson-dataformat-xml} is no longer a dependency
 * of this module, and it is the one documented break in the alias.</p>
 */
@SuppressWarnings("deprecation")
public class RosettaXmlMapperCompatibilityTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/extension-schema-xml-config.json";

    private static RosettaXMLConfiguration loadConfig() throws Exception {
        try (InputStream is = Resources.getResource(XML_CONFIG).openStream()) {
            return RosettaXMLConfiguration.load(is);
        }
    }

    private static TopLevel newDocument() {
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        return TopLevel.builder().setAttr(foo).setValue(measure).build();
    }

    /** The old call shape still compiles, and the result is usable as a plain {@link ObjectMapper}. */
    @Test
    public void legacyConstructorStillCompilesAndIsAnObjectMapper() throws Exception {
        ObjectMapper mapper = new RosettaXmlMapper(
                loadConfig(), RosettaXmlMapperCompatibilityTest.class.getClassLoader());

        assertInstanceOf(StaxXmlObjectMapper.class, mapper,
                "the alias must resolve to the StAX-backed facade");

        TopLevel document = newDocument();
        assertEquals(document, mapper.readValue(mapper.writeValueAsString(document), TopLevel.class));
    }

    /**
     * Unlike the class it replaces, the alias is fully configured on construction: the old
     * {@code RosettaXmlMapper} produced no Rosetta-aware XML without the (deleted)
     * {@code RosettaSerialiserFactory} and {@code RosettaXMLModule} bolted on. Its output must now
     * be identical to what {@code forXML(...)} produces.
     */
    @Test
    public void producesIdenticalOutputToTheSupportedEntryPoint() throws Exception {
        ObjectMapper alias = new RosettaXmlMapper(
                loadConfig(), RosettaXmlMapperCompatibilityTest.class.getClassLoader());
        ObjectMapper supported;
        try (InputStream is = Resources.getResource(XML_CONFIG).openStream()) {
            supported = RosettaObjectMapperCreator.forXML(is).create();
        }

        TopLevel document = newDocument();

        assertEquals(supported.writeValueAsString(document), alias.writeValueAsString(document));
        assertEquals(
                supported.writerWithDefaultPrettyPrinter()
                        .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd")
                        .writeValueAsString(document),
                alias.writerWithDefaultPrettyPrinter()
                        .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd")
                        .writeValueAsString(document));
    }
}
