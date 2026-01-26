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
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.test.*;
import com.rosetta.util.DottedPath;
import com.rosetta.util.serialisation.AttributeXMLConfiguration;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import com.rosetta.util.serialisation.TypeXMLConfiguration;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlSerialisationTest {
    private static final String XSD_SCHEMA = "/xml-serialisation/schema/extension-schema.xsd";

    private final Validator xsdValidator;
    private final ObjectMapper xmlMapper;
    private final URL configUrl;

    public XmlSerialisationTest() throws SAXException, IOException {
        URL schemaFile = XmlSerialisationTest.class.getResource(XSD_SCHEMA);
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        xsdValidator = schema.newValidator();

        // Create an XML mapper with the generated XML configuration based on the XSD schema
        configUrl = Resources.getResource("xml-serialisation/xml-config/extension-schema-xml-config.json");
        try (InputStream inputStream = configUrl.openStream()) {
            xmlMapper = RosettaObjectMapperCreator.forXML(inputStream).create();
        }
    }

    @Test
    public void testDocumentSerialisation() throws SAXException, IOException {
        // Construct a Document object
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
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
    public void testTopLevelXsdExtensionSerialisation() throws SAXException, IOException {
        // Construct a Document object
        Foo foo = Foo.builder().setXmlValue("My value").addAttr1("Foo").addAttr1("Bar").build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        Document document = TopLevelExtension.builder().setAttr(foo).setValue(measure).setDocumentExtensionAttr("Document Extension Attribute Value").build();

        // Test serialisation
        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        ObjectWriter xmlWriter = xmlMapper
                .writerWithDefaultPrettyPrinter()
                .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd");
        String actualXML = licenseHeader + xmlWriter.writeValueAsString(document);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/extended-top-level-document.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test serialised document matches the XSD schema
        xsdValidator.validate(new StreamSource(new ByteArrayInputStream(actualXML.getBytes(StandardCharsets.UTF_8))));

        // Test TopLevelExtension is a type of DocumentExtension which is what we need to deserialize to
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
    public void testZonedDateTimeWithUnknowTimeAndUnknownTimezoneDeserialisation() throws JsonProcessingException {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("Unknown"));
        String xml = "<ZonedDateTime>2006-04-02</ZonedDateTime>";

        // Test deserialisation
        ZonedDateTime actual = xmlMapper.readValue(xml, ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknowTimeAndZuluTimezoneDeserialisation() throws JsonProcessingException {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("Z"));
        String xml = "<ZonedDateTime>2006-04-02Z</ZonedDateTime>";

        // Test deserialisation
        ZonedDateTime actual = xmlMapper.readValue(xml, ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknowTimeAndStandardOffsetTimezoneDeserialisation() throws JsonProcessingException {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("+01:00"));
        String xml = "<ZonedDateTime>2006-04-02+01:00</ZonedDateTime>";

        // Test deserialisation
        ZonedDateTime actual = xmlMapper.readValue(xml, ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknowTimeAndCompactOffsetTimezoneDeserialisation() throws JsonProcessingException {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("+0100"));
        String xml = "<ZonedDateTime>2006-04-02+0100</ZonedDateTime>";

        // Test deserialisation
        ZonedDateTime actual = xmlMapper.readValue(xml, ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeWithUnknowTimeAndShortOffsetTimezoneDeserialisation() throws JsonProcessingException {
        ZonedDateTime expected = ZonedDateTime.of(2006, 4, 2, 0, 0, 0, 0, ZoneId.of("+01"));
        String xml = "<ZonedDateTime>2006-04-02+01</ZonedDateTime>";

        // Test deserialisation
        ZonedDateTime actual = xmlMapper.readValue(xml, ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testElementNamedTypeDeserialisation() throws IOException {
        TypeWithTypeElement t = TypeWithTypeElement.builder()
                .setFirstElement("first")
                .setType("My type")
                .build();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(t);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/element-named-type.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        TypeWithTypeElement actual = xmlMapper.readValue(expectedXML, TypeWithTypeElement.class);
        assertEquals(t, actual);
    }

    @Test
    public void testMultiCardinalitySerialisation() throws IOException {
        // Construct a MultiCardinality object
        MulticardinalityContainer multicardinalityContainer = MulticardinalityContainer.builder()
                .addFoo(Foo.builder().setXmlValue("foo1").addAttr1("Foo").addAttr1("Bar").build())
                .addFoo(Foo.builder().setXmlValue("foo2").addAttr1("Qux").build())
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
    public void testPolymorphicDeserialisation() throws IOException {
        String input = Resources.toString(Resources.getResource("xml-serialisation/input/polymorphic.xml"), StandardCharsets.UTF_8);

        // Test deserialisation
        AnimalContainer actual = xmlMapper.readValue(input, AnimalContainer.class);

        AnimalContainer expected = AnimalContainer.builder()
                .setAnimal(Snake.builder().setName("Snakee")
                        .setSnakeModel(SnakeModel.builder()
                                .setDeadliness(SnakeDeadlinessEnum.DEADLY))).build();

        assertEquals(expected, actual);
    }

    @Test
    public void testPolymorphicReplacementDeserialisation() throws IOException {
        String input = Resources.toString(Resources.getResource("xml-serialisation/input/polymorphic-replacement.xml"), StandardCharsets.UTF_8);

        // Test deserialisation
        AnimalContainer actual = xmlMapper.readValue(input, AnimalContainer.class);

        AnimalContainer expected = AnimalContainer.builder()
                .setAnimal(com.rosetta.extension.test.Snake.builder().setName("Snakee")
                                .setSnakeExtensionModel(SnakeExtensionModel.builder().setDeadliness("MostlyHarmless"))
                        ).build();

        assertEquals(expected, actual);
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
    public void testMultiCardinalitySubstitutionGroupSerialisation() throws IOException {
        Zoo zoo = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .addAnimal(Shark.builder().setName("Jaws").build())
                .addAnimal(Salmon.builder().setName("Sashimi").build())
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

    @Test
    public void testMulticardinalityAttributeInsideVirtualAttribute() throws IOException {
        Party party = Party.builder()
                .setPartyNameModel(PartyNameModel.builder()
                        .setPartyName("my name"))
                .setPartyModel(PartyModel.builder()
                        .addPartyId("myId1")
                        .addPartyId("myId2"))
                .build();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + xmlMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(party);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/virtual-attributes.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        Party actual = xmlMapper.readValue(expectedXML, Party.class);
        assertEquals(party, actual);
    }

    @Test
    public void testSubstitutionGroupLegacyV2Serialisation() throws IOException {
        AnimalContainer animalContainer = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();

        ObjectMapper legacyObjectMapper = getLegacyV2ObjectMapper();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + legacyObjectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(animalContainer);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/substitution-group.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        AnimalContainer actual = legacyObjectMapper.readValue(expectedXML, AnimalContainer.class);
        assertEquals(animalContainer, actual);
    }

    @Test
    public void testMultiCardinalitySubstitutionGroupLegacyV2Serialisation() throws IOException {
        Zoo zoo = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .build();

        ObjectMapper legacyObjectMapper = getLegacyV2ObjectMapper();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + legacyObjectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(zoo);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/substitution-group-multi-legacy.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        Zoo actual = legacyObjectMapper.readValue(expectedXML, Zoo.class);
        assertEquals(zoo, actual);
    }

    @Test
    public void testSubstitutionGroupLegacyV1Serialisation() throws IOException {
        AnimalContainer animalContainer = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();

        ObjectMapper legacyObjectMapper = getLegacyV1ObjectMapper();

        String licenseHeader = Resources.toString(Resources.getResource("xml-serialisation/expected/license-header.xml"), StandardCharsets.UTF_8);
        // Test serialisation
        String actualXML = licenseHeader + legacyObjectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(animalContainer);
        String expectedXML = Resources.toString(Resources.getResource("xml-serialisation/expected/substitution-group.xml"), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test deserialisation
        AnimalContainer actual = legacyObjectMapper.readValue(expectedXML, AnimalContainer.class);
        assertEquals(animalContainer, actual);
    }

    private ObjectMapper getLegacyV1ObjectMapper() {
        return RosettaObjectMapperCreator.forXML(getLegacyV1RosettaXMLConfiguration()).create();
    }

    private RosettaXMLConfiguration getLegacyV1RosettaXMLConfiguration() {
        try (InputStream inputStream = configUrl.openStream()) {
            final RosettaXMLConfiguration config = RosettaXMLConfiguration.load(inputStream);
            Map<ModelSymbolId, TypeXMLConfiguration> newTypeConfigMap = new HashMap<>();

            config.getTypeConfigMap().forEach((modelSymbolId, typeXMLConfiguration) -> {
                if (!typeXMLConfiguration.getAbstract().orElse(false)) {
                    Optional<Map<String, AttributeXMLConfiguration>> newAttributeXmlConfiguration = typeXMLConfiguration.getAttributes()
                            .map(attributes -> {
                                Map<String, AttributeXMLConfiguration> newAttributes = new HashMap<>();
                                attributes.forEach((key, attr) -> {
                                    AttributeXMLConfiguration newAttributeConfiguration = new AttributeXMLConfiguration(attr.getXmlName(),
                                            attr.getXmlAttributes(),
                                            attr.getXmlRepresentation(),
                                            attr.getElementRef(), //populate substitution group with what is now elementRef as per legacy format
                                            Optional.empty());
                                    newAttributes.put(key, newAttributeConfiguration);
                                });
                                return newAttributes;
                            });

                    TypeXMLConfiguration newTypeXmlConfiguration = new TypeXMLConfiguration(
                            deriveLegacyV1SubstitutionFor(modelSymbolId, typeXMLConfiguration), //populate with type to substitute for as in v1 format
                            Optional.empty(), //empty substitution group as in v1 format
                            typeXMLConfiguration.getXmlElementName(),
                            Optional.empty(), //blank out XmlElementFullyQualifiedName as per legacy format
                            Optional.empty(), //blank out abstract as per legacy format
                            typeXMLConfiguration.getXmlAttributes(),
                            newAttributeXmlConfiguration,
                            typeXMLConfiguration.getEnumValues()
                    );
                    newTypeConfigMap.put(modelSymbolId, newTypeXmlConfiguration);
                }
            });

            return new RosettaXMLConfiguration(newTypeConfigMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ModelSymbolId> deriveLegacyV1SubstitutionFor(ModelSymbolId modelSymbolId, TypeXMLConfiguration typeXMLConfiguration) {
        return typeXMLConfiguration.getSubstitutionGroup()
                .map(substitutionGroup -> {
                    String name = substitutionGroup.replaceAll(".*/(.*?)$", "$1");
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    DottedPath namespace = modelSymbolId.getNamespace();
                    return new ModelSymbolId(namespace, name);
                });
    }


    private ObjectMapper getLegacyV2ObjectMapper() {
        return RosettaObjectMapperCreator.forXML(getLegacyV2RosettaXMLConfiguration()).create();
    }

    private RosettaXMLConfiguration getLegacyV2RosettaXMLConfiguration() {
        try (InputStream inputStream = configUrl.openStream()) {
            final RosettaXMLConfiguration config = RosettaXMLConfiguration.load(inputStream);
            Map<ModelSymbolId, TypeXMLConfiguration> newTypeConfigMap = new HashMap<>();

            config.getTypeConfigMap().forEach((modelSymbolId, typeXMLConfiguration) -> {
                if (!typeXMLConfiguration.getAbstract().orElse(false)) {
                    Optional<Map<String, AttributeXMLConfiguration>> newAttributeXmlConfiguration = typeXMLConfiguration.getAttributes()
                            .map(attributes -> {
                                Map<String, AttributeXMLConfiguration> newAttributes = new HashMap<>();
                                attributes.forEach((key, attr) -> {
                                    AttributeXMLConfiguration newAttributeConfiguration = new AttributeXMLConfiguration(attr.getXmlName(),
                                            attr.getXmlAttributes(),
                                            attr.getXmlRepresentation(),
                                            attr.getElementRef(), //populate substitution group with what is now elementRef as per legacy format
                                            Optional.empty());
                                    newAttributes.put(key, newAttributeConfiguration);
                                });
                                return newAttributes;
                            });

                    TypeXMLConfiguration newTypeXmlConfiguration = new TypeXMLConfiguration(
                            typeXMLConfiguration.getSubstitutionFor(),
                            typeXMLConfiguration.getSubstitutionGroup(),
                            typeXMLConfiguration.getXmlElementName(),
                            Optional.empty(), //blank out XmlElementFullyQualifiedName as per legacy format
                            Optional.empty(), //blank out abstract as per legacy format
                            typeXMLConfiguration.getXmlAttributes(),
                            newAttributeXmlConfiguration,
                            typeXMLConfiguration.getEnumValues()
                    );
                    newTypeConfigMap.put(modelSymbolId, newTypeXmlConfiguration);
                }
            });

            return new RosettaXMLConfiguration(newTypeConfigMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
