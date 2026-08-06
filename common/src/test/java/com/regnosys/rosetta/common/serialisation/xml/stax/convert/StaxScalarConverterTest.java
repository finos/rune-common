package com.regnosys.rosetta.common.serialisation.xml.stax.convert;

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

import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.records.Date;
import com.rosetta.test.SnakeDeadlinessEnum;
import com.rosetta.test.UnitEnum;
import com.rosetta.util.DottedPath;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link StaxScalarConverter} covering all scalar types and date/time formats.
 * Time/ZonedDateTime cases mirror the expectations in XmlSerialisationTest.
 */
public class StaxScalarConverterTest {

    private static final String XML_CONFIG = "serialisation/xml/xml-config/extension-schema-xml-config.json";

    private StaxScalarConverter converter;

    @BeforeEach
    public void setUp() throws IOException {
        URL configUrl = Resources.getResource(XML_CONFIG);
        RosettaXMLConfiguration config;
        try (InputStream in = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(in);
        }
        converter = new StaxScalarConverter(config);
    }

    // -------------------------------------------------------------------------
    // String, primitives, BigDecimal
    // -------------------------------------------------------------------------

    @Test
    public void testString_roundTrip() {
        assertEquals("hello", converter.toXmlString("hello"));
        assertEquals("hello", converter.fromXmlString("hello", String.class));
    }

    @Test
    public void testBigDecimal_toXmlString() {
        assertEquals("1", converter.toXmlString(BigDecimal.ONE));
        assertEquals("3.14", converter.toXmlString(new BigDecimal("3.14")));
    }

    @Test
    public void testBigDecimal_avoidsSciNotation() {
        assertEquals("0.000001", converter.toXmlString(new BigDecimal("0.000001")));
    }

    @Test
    public void testBigDecimal_fromXmlString() {
        assertEquals(new BigDecimal("42.5"), converter.fromXmlString("42.5", BigDecimal.class));
    }

    @Test
    public void testInteger_roundTrip() {
        assertEquals("42", converter.toXmlString(42));
        assertEquals(42, converter.fromXmlString("42", Integer.class));
        assertEquals(42, converter.fromXmlString("42", int.class));
    }

    @Test
    public void testBoolean_roundTrip() {
        assertEquals("true", converter.toXmlString(true));
        assertEquals("false", converter.toXmlString(false));
        assertEquals(true, converter.fromXmlString("true", Boolean.class));
        assertEquals(false, converter.fromXmlString("false", boolean.class));
    }

    // -------------------------------------------------------------------------
    // Rune Date
    // -------------------------------------------------------------------------

    @Test
    public void testDate_toXmlString() {
        assertEquals("2026-05-09", converter.toXmlString(Date.of(2026, 5, 9)));
    }

    @Test
    public void testDate_fromXmlString() {
        assertEquals(Date.of(2026, 5, 9), converter.fromXmlString("2026-05-09", Date.class));
    }

    // -------------------------------------------------------------------------
    // LocalTime — mirrors XmlSerialisationTest.testTime*
    // -------------------------------------------------------------------------

    @Test
    public void testLocalTime_serialisation() {
        // LocalTime.of(1, 23, 45) → "01:23:45Z" (UTC offset appended)
        assertEquals("01:23:45Z", converter.toXmlString(LocalTime.of(1, 23, 45)));
    }

    @Test
    public void testLocalTime_deserialisationWithoutTimezone() {
        // "01:23:45" → LocalTime.of(1, 23, 45)
        assertEquals(LocalTime.of(1, 23, 45), converter.fromXmlString("01:23:45", LocalTime.class));
    }

    @Test
    public void testLocalTime_deserialisationWithUtcZ() {
        // "01:23:45Z" → LocalTime.of(1, 23, 45)
        assertEquals(LocalTime.of(1, 23, 45), converter.fromXmlString("01:23:45Z", LocalTime.class));
    }

    @Test
    public void testLocalTime_deserialisationWithTimeOffset() {
        // "03:23:45+02:00" → UTC-adjusted to LocalTime.of(1, 23, 45)
        assertEquals(LocalTime.of(1, 23, 45), converter.fromXmlString("03:23:45+02:00", LocalTime.class));
    }

    // -------------------------------------------------------------------------
    // ZonedDateTime — mirrors XmlSerialisationTest.testZonedDateTime*
    // -------------------------------------------------------------------------

    @Test
    public void testZonedDateTime_unknownZone_serialisation() {
        ZonedDateTime value = ZonedDateTime.of(2006, 4, 2, 15, 38, 0, 0, ZoneId.of("Unknown"));
        assertEquals("2006-04-02T15:38:00", converter.toXmlString(value));
    }

    @Test
    public void testZonedDateTime_unknownZone_deserialisation() {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 15, 38, 0, 0, ZoneId.of("Unknown"));
        assertEquals(expected, converter.fromXmlString("2006-04-02T15:38:00", ZonedDateTime.class));
    }

    @Test
    public void testZonedDateTime_localDateOnly_deserialisation() {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("Unknown"));
        assertEquals(expected, converter.fromXmlString("2006-04-02", ZonedDateTime.class));
    }

    @Test
    public void testZonedDateTime_dateWithZuluOffset_deserialisation() {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("Z"));
        assertEquals(expected, converter.fromXmlString("2006-04-02Z", ZonedDateTime.class));
    }

    @Test
    public void testZonedDateTime_dateWithStandardOffset_deserialisation() {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("+01:00"));
        assertEquals(expected, converter.fromXmlString("2006-04-02+01:00", ZonedDateTime.class));
    }

    @Test
    public void testZonedDateTime_dateWithCompactOffset_deserialisation() {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("+0100"));
        assertEquals(expected, converter.fromXmlString("2006-04-02+0100", ZonedDateTime.class));
    }

    @Test
    public void testZonedDateTime_dateWithShortOffset_deserialisation() {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("+01"));
        assertEquals(expected, converter.fromXmlString("2006-04-02+01", ZonedDateTime.class));
    }

    // -------------------------------------------------------------------------
    // Enum — with config override (UnitEnum has enumValues in the test config)
    // -------------------------------------------------------------------------

    @Test
    public void testEnum_serialise_withConfigOverride() {
        // Config maps "METER" → "Meter"; displayName is also "Meter"
        assertEquals("Meter", converter.toXmlString(UnitEnum.METER));
        assertEquals("Kilogram", converter.toXmlString(UnitEnum.KILOGRAM));
    }

    @Test
    public void testEnum_deserialise_withConfigOverride() {
        assertEquals(UnitEnum.METER, converter.fromXmlString("Meter", UnitEnum.class));
        assertEquals(UnitEnum.KILOGRAM, converter.fromXmlString("Kilogram", UnitEnum.class));
    }

    @Test
    public void testEnum_serialise_noConfig() {
        // Without a config, falls back to toDisplayString() which returns displayName
        StaxScalarConverter noConfig = new StaxScalarConverter(new RosettaXMLConfiguration(
                new java.util.HashMap<ModelSymbolId, com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration>()));
        assertEquals("Deadly", noConfig.toXmlString(SnakeDeadlinessEnum.DEADLY));
    }

    @Test
    public void testEnum_deserialise_noConfig_viaDisplayName() {
        StaxScalarConverter noConfig = new StaxScalarConverter(new RosettaXMLConfiguration(
                new java.util.HashMap<ModelSymbolId, com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration>()));
        assertEquals(SnakeDeadlinessEnum.DEADLY, noConfig.fromXmlString("Deadly", SnakeDeadlinessEnum.class));
    }
}
