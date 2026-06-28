package com.regnosys.rosetta.common.serialisation.xml.stax.read;

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
import com.rosetta.model.lib.records.Date;
import com.rosetta.test.DateAttributeContainer;
import com.rosetta.test.Document;
import com.rosetta.test.Foo;
import com.rosetta.test.Measure;
import com.rosetta.test.MulticardinalityContainer;
import com.rosetta.test.Party;
import com.rosetta.test.PartyModel;
import com.rosetta.test.PartyNameModel;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link StaxReader} covering the basic deserialisation scenarios
 * (Step 4a of the StAX migration plan).
 *
 * <p>These tests deliberately exclude substitution-group and content-model
 * disambiguation cases, which are covered by Steps 4b and 4c respectively.
 */
public class StaxReaderTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/extension-schema-xml-config.json";

    private StaxReader reader;

    @BeforeEach
    public void setUp() throws Exception {
        URL configUrl = Resources.getResource(XML_CONFIG);
        RosettaXMLConfiguration config;
        try (InputStream is = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(is);
        }
        reader = new StaxReader(config, getClass().getClassLoader());
    }

    // -------------------------------------------------------------------------
    // Basic nested object round-trip
    // -------------------------------------------------------------------------

    @Test
    public void testDocumentDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource("serialisation/xml/expected/document.xml"),
                StandardCharsets.UTF_8);

        TopLevel actual = reader.read(xml, TopLevel.class);

        Foo expectedFoo = Foo.builder()
                .setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure expectedMeasure = Measure.builder()
                .setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        TopLevel expected = TopLevel.builder()
                .setAttr(expectedFoo).setValue(expectedMeasure).build();

        assertEquals(expected, actual);
    }

    @Test
    public void testTopLevelExtensionDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource("serialisation/xml/expected/extended-top-level-document.xml"),
                StandardCharsets.UTF_8);

        // Caller asks for Document; root element is <TopLevelExtension> — infer concrete type
        Document actual = reader.read(xml, Document.class);

        Foo expectedFoo = Foo.builder()
                .setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure expectedMeasure = Measure.builder()
                .setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        Document expected = TopLevelExtension.builder()
                .setAttr(expectedFoo)
                .setValue(expectedMeasure)
                .setDocumentExtensionAttr("Document Extension Attribute Value")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void testPrunesEmptyNestedObject() throws Exception {
        String xml = Resources.toString(
                Resources.getResource("serialisation/xml/expected/top-level-prune.xml"),
                StandardCharsets.UTF_8);

        TopLevel actual = reader.read(xml, TopLevel.class);

        assertEquals(BigDecimal.ONE, actual.getValue().getValue());
        assertEquals(UnitEnum.METER, actual.getValue().getUnit());
        // empty <Attr/> must be pruned to null
        assertNull(actual.getAttr());
    }

    // -------------------------------------------------------------------------
    // Attribute representation (XML attribute → field)
    // -------------------------------------------------------------------------

    @Test
    public void testDateAttribute() throws Exception {
        String xml = "<DateAttributeContainer TradeDate=\"2026-05-09\"/>";

        DateAttributeContainer actual = reader.read(xml, DateAttributeContainer.class);

        DateAttributeContainer expected = DateAttributeContainer.builder()
                .setTradeDate(Date.of(2026, 5, 9)).build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // VALUE representation (text content → field) + ATTRIBUTE representation
    // -------------------------------------------------------------------------

    @Test
    public void testMeasureValueAndAttribute() throws Exception {
        // Reads both text content (value) and XML attribute (unit) from same element
        String xml = "<Measure Unit=\"Meter\">1</Measure>";

        Measure actual = reader.read(xml, Measure.class);

        assertEquals(BigDecimal.ONE, actual.getValue());
        assertEquals(UnitEnum.METER, actual.getUnit());
    }

    // -------------------------------------------------------------------------
    // LocalTime handling
    // -------------------------------------------------------------------------

    @Test
    public void testTimeDeserialisation() throws Exception {
        String xml = "<TimeContainer><timeValue>01:23:45Z</timeValue></TimeContainer>";

        TimeContainer actual = reader.read(xml, TimeContainer.class);

        TimeContainer expected = TimeContainer.builder()
                .setTimeValue(LocalTime.of(1, 23, 45)).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeDeserialisationWithoutTimezone() throws Exception {
        String xml = "<TimeContainer><timeValue>01:23:45</timeValue></TimeContainer>";

        TimeContainer actual = reader.read(xml, TimeContainer.class);

        TimeContainer expected = TimeContainer.builder()
                .setTimeValue(LocalTime.of(1, 23, 45)).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeDeserialisationWithTimeOffset() throws Exception {
        String xml = "<TimeContainer><timeValue>03:23:45+02:00</timeValue></TimeContainer>";

        TimeContainer actual = reader.read(xml, TimeContainer.class);

        TimeContainer expected = TimeContainer.builder()
                .setTimeValue(LocalTime.of(1, 23, 45)).build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // ZonedDateTime handling
    // -------------------------------------------------------------------------

    @Test
    public void testZonedDateTimeWithUnknownTimezone() throws Exception {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 15, 38, 0, 0,
                ZoneId.of("Unknown"));
        String xml = "<ZonedDateTime>2006-04-02T15:38:00</ZonedDateTime>";

        ZonedDateTime actual = reader.read(xml, ZonedDateTime.class);

        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimeAndUnknownTimezone() throws Exception {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0,
                ZoneId.of("Unknown"));
        String xml = "<ZonedDateTime>2006-04-02</ZonedDateTime>";

        ZonedDateTime actual = reader.read(xml, ZonedDateTime.class);

        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimeAndZuluTimezone() throws Exception {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0,
                ZoneId.of("Z"));
        String xml = "<ZonedDateTime>2006-04-02Z</ZonedDateTime>";

        ZonedDateTime actual = reader.read(xml, ZonedDateTime.class);

        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimeAndStandardOffsetTimezone() throws Exception {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0,
                ZoneId.of("+01:00"));
        String xml = "<ZonedDateTime>2006-04-02+01:00</ZonedDateTime>";

        ZonedDateTime actual = reader.read(xml, ZonedDateTime.class);

        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimeAndCompactOffsetTimezone() throws Exception {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0,
                ZoneId.of("+0100"));
        String xml = "<ZonedDateTime>2006-04-02+0100</ZonedDateTime>";

        ZonedDateTime actual = reader.read(xml, ZonedDateTime.class);

        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimeAndShortOffsetTimezone() throws Exception {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0,
                ZoneId.of("+01"));
        String xml = "<ZonedDateTime>2006-04-02+01</ZonedDateTime>";

        ZonedDateTime actual = reader.read(xml, ZonedDateTime.class);

        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Element named "type" — getType() exclusion (related to criterion 13)
    // -------------------------------------------------------------------------

    /**
     * Verifies that a Rune type with a data field named "type" is correctly deserialised.
     * The introspector excludes the meta {@code getType()} method from RosettaModelObject
     * and maps the data getter {@code _getType()} (with logical name "type") to the
     * {@code <type>} child element.
     */
    @Test
    public void testElementNamedTypeDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource("serialisation/xml/expected/element-named-type.xml"),
                StandardCharsets.UTF_8);

        TypeWithTypeElement actual = reader.read(xml, TypeWithTypeElement.class);

        TypeWithTypeElement expected = TypeWithTypeElement.builder()
                .setFirstElement("first").setType("My type").build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Multi-cardinality
    // -------------------------------------------------------------------------

    @Test
    public void testMultiCardinalityDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource("serialisation/xml/expected/multicardinality-container.xml"),
                StandardCharsets.UTF_8);

        MulticardinalityContainer actual = reader.read(xml, MulticardinalityContainer.class);

        MulticardinalityContainer expected = MulticardinalityContainer.builder()
                .addFoo(Foo.builder().setXmlValue("foo1")
                        .addAttr1("Foo").addAttr1("Bar").build())
                .addFoo(Foo.builder().setXmlValue("foo2")
                        .addAttr1("Qux").build())
                .build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // VIRTUAL attributes (transparent wrapper types)
    // -------------------------------------------------------------------------

    /**
     * Party has two VIRTUAL attributes: partyNameModel and partyModel.
     * Their children (partyName, partyId) appear directly in the &lt;Party&gt; element
     * with no wrapper element. The reader must route them through the virtual builders.
     */
    @Test
    public void testVirtualAttributeDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource("serialisation/xml/expected/virtual-attributes.xml"),
                StandardCharsets.UTF_8);

        Party actual = reader.read(xml, Party.class);

        Party expected = Party.builder()
                .setPartyNameModel(PartyNameModel.builder().setPartyName("my name"))
                .setPartyModel(PartyModel.builder()
                        .addPartyId("myId1").addPartyId("myId2"))
                .build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Criterion 13 — XML attribute vs XML element with the same local name
    // -------------------------------------------------------------------------

    /**
     * Criterion 13: a type with both an XML ATTRIBUTE and an XML ELEMENT sharing the
     * same local name. The StAX reader natively distinguishes them — XML attributes are
     * read from the START_ELEMENT token; child elements are START_ELEMENT events in the
     * child-event loop. The Measure type exercises this pattern: "Unit" is an ATTRIBUTE
     * and a hypothetical "Unit" element below would also be readable independently.
     *
     * <p>This test uses the {@link DateAttributeContainer} type which has "TradeDate" as
     * an XML ATTRIBUTE. It confirms that an unknown child element with the same name as
     * the XML attribute is silently skipped (not confused with the attribute), and the
     * attribute value is still correctly populated.
     */
    @Test
    public void testAttributeAndElementSameLocalNameAreDistinct() throws Exception {
        // TradeDate is an XML ATTRIBUTE on DateAttributeContainer.
        // An extra <TradeDate> child element should be silently skipped.
        String xml = "<DateAttributeContainer TradeDate=\"2026-05-09\">"
                + "<TradeDate>2099-01-01</TradeDate>"
                + "</DateAttributeContainer>";

        DateAttributeContainer actual = reader.read(xml, DateAttributeContainer.class);

        // Attribute value "2026-05-09" must win; element "2099-01-01" must be ignored.
        DateAttributeContainer expected = DateAttributeContainer.builder()
                .setTradeDate(Date.of(2026, 5, 9)).build();
        assertEquals(expected, actual);
    }
}
