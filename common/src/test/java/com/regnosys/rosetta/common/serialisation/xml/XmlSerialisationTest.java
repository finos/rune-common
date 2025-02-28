package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.test.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlSerialisationTest {
    private static final String XSD_SCHEMA = "/xml-serialisation/schema/schema.xsd";

    private final Validator xsdValidator;
    private final ObjectMapper xmlMapper;

    public XmlSerialisationTest() throws SAXException, IOException {
        URL schemaFile = XmlSerialisationTest.class.getResource(XSD_SCHEMA);
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        xsdValidator = schema.newValidator();

        // Create an XML mapper with the generated XML configuration based on the XSD schema
        URL url = Resources.getResource("xml-serialisation/xml-config.json");
        try (InputStream inputStream = url.openStream()) {
            xmlMapper = RosettaObjectMapperCreator.forXML(inputStream).create();
        }
    }

    @Test
    public void testDocumentSerialisation() throws SAXException, IOException {
        // Construct a Document object
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1(1).addAttr1(2).build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        TopLevel document = TopLevel.builder().setAttr(foo).setValue(measure).build();

        // Test serialisation
        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        ObjectWriter xmlWriter = xmlMapper
                .writerWithDefaultPrettyPrinter()
                .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd");
        String actualXML = licenseHeader + xmlWriter.writeValueAsString(document);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/document.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test serialised document matches the XSD schema
        xsdValidator.validate(new StreamSource(new ByteArrayInputStream(actualXML.getBytes(StandardCharsets.UTF_8))));

        // Test deserialisaton
        TopLevel actual = xmlMapper.readValue(expectedXML, TopLevel.class);
        assertEquals(document, actual);
    }

    @Test
    public void testTimeSerialisation() throws JsonProcessingException {
        // Construct a TimeContainer object
        TimeContainer timeContainer = TimeContainer.builder().setTimeValue(LocalTime.of(1, 23, 45)).build();

        // Test serialisation
        String actualXML = xmlMapper.writeValueAsString(timeContainer);
        String expectedXML = "<TimeContainer><timeValue>01:23:45Z</timeValue></TimeContainer>";
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        TimeContainer actual = xmlMapper.readValue(expectedXML, TimeContainer.class);
        assertEquals(timeContainer, actual);
    }

    @Test
    public void testTimeDeserialisationWithoutTimezone() throws JsonProcessingException {
        TimeContainer expected = TimeContainer.builder().setTimeValue(LocalTime.of(1, 23, 45)).build();
        String xml = "<TimeContainer><timeValue>01:23:45</timeValue></TimeContainer>";

        // Test deserialisation
        TimeContainer actual = xmlMapper.readValue(xml, TimeContainer.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeDeserialisationWithTimeOffset() throws JsonProcessingException {
        TimeContainer expected = TimeContainer.builder().setTimeValue(LocalTime.of(1, 23, 45)).build();
        String xml = "<TimeContainer><timeValue>03:23:45+02:00</timeValue></TimeContainer>";

        // Test deserialisation
        TimeContainer actual = xmlMapper.readValue(xml, TimeContainer.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimezoneDeserialisation() throws JsonProcessingException {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 15, 38, 0, 0, ZoneId.of("Unknown"));
        String xml = "<ZonedDateTime>2006-04-02T15:38:00</ZonedDateTime>";

        // Test deserialisation
        ZonedDateTime actual = xmlMapper.readValue(xml, ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknownTimezoneSerialisation() throws JsonProcessingException {
        String expected = "<ZonedDateTime>2006-04-02T15:38:00</ZonedDateTime>";
        ZonedDateTime value = ZonedDateTime.of(2006, 4, 2, 15, 38, 0, 0, ZoneId.of("Unknown"));

        // Test serialisation
        String actual = xmlMapper.writeValueAsString(value);
        assertEquals(expected, actual);
    }

    @Test
    public void testMultiCardinalitySerialisation() throws IOException {
        // Construct a MultiCardinality object
        MulticardinalityContainer multicardinalityContainer = MulticardinalityContainer.builder()
                .addFoo(Foo.builder().setXmlValue("foo1").addAttr1(1).addAttr1(2).build())
                .addFoo(Foo.builder().setXmlValue("foo2").addAttr1(3).build())
                .build();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(multicardinalityContainer);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/multicardinality-container.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        MulticardinalityContainer actual = xmlMapper.readValue(expectedXML, MulticardinalityContainer.class);
        assertEquals(multicardinalityContainer, actual);
    }

    @Test
    @Disabled // TODO
    public void testNestedContainerSerialisation() throws IOException {
        // Construct a MultiCardinality object
        NestedContainer nestedContainer = NestedContainer.builder()
                .setNestedContainerSequence0(NestedContainerSequence0.builder().setA(0).setB(1).build())
                .addNestedContainerSequence1(NestedContainerSequence1.builder().setC(2).setD(3).build())
                .addNestedContainerSequence1(NestedContainerSequence1.builder().setC(4).setD(5).build())
                .build();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(nestedContainer);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/nested-container.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        NestedContainer actual = xmlMapper.readValue(expectedXML, NestedContainer.class);
        assertEquals(nestedContainer, actual);
    }

    @Test
    // TODO: test non-substituted groups should not perform substitution
    public void testSubstitutionGroupSerialisation() throws IOException {
        AnimalContainer animalContainer = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(animalContainer);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/substitution-group.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        AnimalContainer actual = xmlMapper.readValue(expectedXML, AnimalContainer.class);
        assertEquals(animalContainer, actual);
    }

    @Test
    public void testMulticardinalitySubstitutionGroupSerialisation() throws IOException {
        Zoo zoo = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .build();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(zoo);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/substitution-group-multi.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        Zoo actual = xmlMapper.readValue(expectedXML, Zoo.class);
        assertEquals(zoo, actual);
    }
}
