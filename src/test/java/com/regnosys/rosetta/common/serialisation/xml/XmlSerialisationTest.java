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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
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
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    @Disabled
    public void test() throws JsonProcessingException {
        ObjectMapper mapper = new XmlMapper((JacksonXmlModule) null) // See issue https://github.com/FasterXML/jackson-dataformat-xml/issues/678
                .setSerializerFactory(RosettaSerialiserFactory.INSTANCE)
                .registerModule(new JacksonXmlModule());

        Document document =
                new Document()
                        .setMyABPair(new ABPair().setA("A0").setB("B0"))
//                        .setMyABPair2(new ABPair().setA("A1").setB("B1"))
                        .addABPair(new ABPair().setA("A1").setB("B1"))
                        .addABPair(new ABPair().setA("A2").setB("B2"))
                        .addABPair(new ABPair().setA("A3").setB("B3"));
        assertEquals("<document><a>A0</a><b>B0</b><a>A1</a><b>B1</b><a>A2</a><b>B2</b><a>A3</a><b>B3</b></document>", mapper.writeValueAsString(document));

        Document deserialised = mapper.readValue("<document><a>A0</a><b>B0</b><a>A1</a><b>B1</b><a>A2</a><b>B2</b><a>A3</a><b>B3</b></document>", Document.class);
        assertEquals("<document><a>A0</a><b>B0</b><a>A1</a><b>B1</b><a>A2</a><b>B2</b><a>A3</a><b>B3</b></document>", mapper.writeValueAsString(deserialised));
    }

    public static class DocumentSerializer extends JsonSerializer<Document> {
        @Override
        public void serialize(Document document, JsonGenerator g, SerializerProvider provider) throws IOException {
            g.writeStartObject();
            for (ABPair value : document.getABPairs()) {
                g.writeFieldName("a");
                g.writeString(value.getA());
                g.writeFieldName("b");
                g.writeString(value.getB());
            }
            g.writeEndObject();
        }
    }
    public static class DocumentDeserializer extends JsonDeserializer<Document> {

        @Override
        public Document deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Document document = new Document();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String name = p.getCurrentName();
                p.nextToken(); // move to value
                String value = p.getText();
                if ("a".equals(name)) {
                    ABPair pair = new ABPair().setA(value);
                    document.addABPair(pair);
                } else if ("b".equals(name)) {
                    List<ABPair> pairs = document.getABPairs();
                    if (!pairs.isEmpty()) {
                        ABPair lastPair = pairs.get(pairs.size() - 1);
                        lastPair.setB(value);
                    }
                }
            }
            return document;
        }
    }
    public static class ABPairsSerializer extends JsonSerializer<List<ABPair>> {
        public ABPairsSerializer() {
            super();
        }

        @Override
        public void serialize(List<ABPair> values, JsonGenerator g, SerializerProvider provider) throws IOException {
            JsonSerializer<Object> pairSerializer = provider.findValueSerializer(ABPair.class).unwrappingSerializer(NameTransformer.simpleTransformer("", ""));
            ToXmlGenerator xgen = (ToXmlGenerator) g;
            xgen.setNextIsUnwrapped(true);
//            xml.setNextName(null);
//            g.writeStartArray();
            xgen.writeStartObject();
            for (ABPair value : values) {
                xgen.writeFieldName("a");
                xgen.writeString(value.getA());
                xgen.writeFieldName("b");
                xgen.writeString(value.getB());
            }
            xgen.writeEndObject();
//            g.writeEndArray();
        }
    }

    @JacksonXmlRootElement(localName = "document")
    @JsonPropertyOrder({ "myABPair", "myABPair2", "abPairs" })
//    @RosettaDataType(builder = DocumentBuilder.class)
//    @JsonSerialize(using = DocumentSerializer.class)
//    @JsonDeserialize(using = DocumentDeserializer.class)
    public static class Document {
        private ABPair myABPair = null;
        private ABPair myABPair2 = null;
        private List<ABPair> abPairs = new ArrayList<>();

        @JsonUnwrapped
        public ABPair getMyABPair() {
            return myABPair;
        }
        public Document setMyABPair(ABPair pair) {
            myABPair = pair;
            return this;
        }

        @JsonUnwrapped
        public ABPair getMyABPair2() {
            return myABPair2;
        }
        public Document setMyABPair2(ABPair pair) {
            myABPair2 = pair;
            return this;
        }

        @JsonUnwrapped
        @JacksonXmlElementWrapper(useWrapping = false)
//        @JsonSerialize(using = ABPairsSerializer.class)
        public List<ABPair> getABPairs() {
            return abPairs;
        }
        public Document addABPair(ABPair pair) {
            abPairs.add(pair);
            return this;
        }
    }
    public static class DocumentBuilder implements RosettaModelObjectBuilder {

        @Override
        public <B extends RosettaModelObjectBuilder> B prune() {
            return null;
        }

        @Override
        public void process(RosettaPath rosettaPath, BuilderProcessor builderProcessor) {

        }

        @Override
        public boolean hasData() {
            return false;
        }

        @Override
        public <B extends RosettaModelObjectBuilder> B merge(B b, BuilderMerger builderMerger) {
            return null;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            return null;
        }

        @Override
        public RosettaModelObject build() {
            return null;
        }

        @Override
        public RosettaMetaData<? extends RosettaModelObject> metaData() {
            return null;
        }

        @Override
        public Class<? extends RosettaModelObject> getType() {
            return null;
        }

        @Override
        public void process(RosettaPath rosettaPath, Processor processor) {

        }
    }

    public static class ABPair {
        private String a;
        private String b;
        
        public String getA() {
            return a;
        }
        public String getB() {
            return b;
        }

        public ABPair setA(String a) {
            this.a = a;
            return this;
        }
        public ABPair setB(String b) {
            this.b = b;
            return this;
        }
    }
}
