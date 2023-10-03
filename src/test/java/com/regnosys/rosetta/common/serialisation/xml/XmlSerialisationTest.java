package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlSerialisationTest {
    private static final String XSD_SCHEMA = "/xml-serialisation/schema/schema.xsd";

    private final Validator xsdValidator;

    public XmlSerialisationTest() {
        URL schemaFile = XmlSerialisationTest.class.getResource(XSD_SCHEMA);
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(schemaFile);
            xsdValidator = schema.newValidator();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDocumentToXmlSerialisation() throws IOException, SAXException {
        // Construct a Document object
        Foo foo = Foo.builder().setXmlValue("xmlVal").setAttr1(1).build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        Document document = Document.builder().setAttr(foo).setValue(measure).build();

        // Create an XML mapper with the generated XML configuration based on the XSD schema
        ObjectMapper xmlMapper = RosettaObjectMapper.getRosettaXMLMapper(
                Resources.getResource("xml-serialisation/xml-config.json").openStream());

        // Test serialisation
        String actualXML = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        String expectedXML = Resources.toString(Objects.requireNonNull(XmlSerialisationTest.class.getResource("/xml-serialisation/expected/document.xml")), StandardCharsets.UTF_8);
        assertEquals(expectedXML, actualXML);

        // Test serialised document matches the XSD schema
        xsdValidator.validate(new StreamSource(new ByteArrayInputStream(actualXML.getBytes(StandardCharsets.UTF_8))));

        // Test deserialisaton
        Document actual = xmlMapper.readValue(actualXML, Document.class);
        assertEquals(document, actual);
    }
}
