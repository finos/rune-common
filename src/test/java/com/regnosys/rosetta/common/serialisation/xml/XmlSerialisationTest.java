package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.common.serialisation.mixin.*;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyGlobalKeyFieldsMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyKeyMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyReferenceMixIn;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.test.*;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlSerialisationTest {
    private static final String XSD_SCHEMA = "/xml-serialisation/schema/schema.xsd";

    private Validator xsdValidator;

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

//    @Test
//    public void testXmlSerialisation() throws IOException, SAXException {
//        URL xmlFile = XmlSerialisationTest.class.getResource("/xml-serialisation/input/sample.xml");
//        xsdValidator.validate(new StreamSource(xmlFile.openStream()));
//    }

    @Test
    public void testDocumentToXmlSerialisation() throws IOException {
        Foo foo = Foo.builder().setXmlValue("xmlVal").setAttr1(1).build();
        Measure measure = Measure.builder().setUnit(UnitEnum.METER).setValue(BigDecimal.ONE).build();
        Document document = Document.builder().setAttr(foo).setValue(measure).build();
        ObjectMapper xmlMapper = RosettaObjectMapper.getRosettaXMLMapper(Resources.getResource("xml-serialisation/xml-config.json").openStream());
        String xmlOutput = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);

        String expected = Resources.toString(Objects.requireNonNull(XmlSerialisationTest.class.getResource("/xml-serialisation/expected/document.xml")), StandardCharsets.UTF_8);
        assertEquals(expected, xmlOutput);

        // Differences from what we expect:
        // - we don't know the name of the toplevel tag
        // - capitalisation of `Attr` is wrong
        // - additional element called `type`, which shouldn't be there.
        // - add `xmlns:xsi="<xsd schema>"` to the toplevel tag?
        // - xsi:type="Document"?
        // - ask
    }
}
