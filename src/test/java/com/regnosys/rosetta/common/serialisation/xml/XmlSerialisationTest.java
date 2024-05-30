package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.test.*;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

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
        xmlMapper = RosettaObjectMapperCreator.forXML(
                Resources.getResource("xml-serialisation/xml-config.json").openStream()).create();
    }

    @Test
    public void testDocumentSerialisation() throws SAXException, IOException {
        // Construct a Document object
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1(1).addAttr1(2).build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        Document document = Document.builder().setAttr(foo).setValue(measure).build();

        // Test serialisation
        ObjectWriter xmlWriter = xmlMapper
                .writerWithDefaultPrettyPrinter()
                .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd");
        String actualXML = xmlWriter.writeValueAsString(document);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/document.xml"), StandardCharsets.UTF_8);
        // assertEquals(expectedXML, actualXML);

        // Test serialised document matches the XSD schema
        xsdValidator.validate(new StreamSource(new ByteArrayInputStream(actualXML.getBytes(StandardCharsets.UTF_8))));

        // Test deserialisaton
        Document actual = xmlMapper.readValue(expectedXML, Document.class);
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
    public void testMultiCardinalitySerialisation() throws IOException {
        // Construct a MultiCardinality object
        MulticardinalityContainer multicardinalityContainer = MulticardinalityContainer.builder()
                .addFoo(Foo.builder().setXmlValue("foo1").addAttr1(1).addAttr1(2).build())
                .addFoo(Foo.builder().setXmlValue("foo2").addAttr1(3).build())
                .build();

        // Test serialisation
        String actualXML = xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(multicardinalityContainer);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/multicardinality-container.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        MulticardinalityContainer actual = xmlMapper.readValue(expectedXML, MulticardinalityContainer.class);
        assertEquals(multicardinalityContainer, actual);
    }
}
