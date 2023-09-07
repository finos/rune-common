package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaClass;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaSerialisationTest {

    @Inject
    CodeGeneratorTestHelper codeGeneratorTestHelper;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
    }

    @Test
    void testBasicSerialisation() throws JsonProcessingException {
        String rosetta = "type A:\n" +
                "        attr1 string (0..1)\n" +
                "        attr2 string (0..1)\n";

        String expectedJson = "{\"attr1\":\"foo\",\"attr2\":\"bar\"}";
        assertJsonSerialisation(rosetta, expectedJson, "com.rosetta.test.model.A");
    }

    @Test
    void testBasicSerialisationMultiCard() throws JsonProcessingException {
        String rosetta = "type A:\n" +
                "        attr1 string (0..*)\n" +
                "        attr2 string (0..*)\n";

        String expectedJson = "{\"attr1\":[\"foo1\",\"foo2\"],\"attr2\":[\"bar\"]}";
        assertJsonSerialisation(rosetta, expectedJson, "com.rosetta.test.model.A");
    }

    @Test
    void testSerialisationWithUpperCaseAttribute() throws JsonProcessingException {
        String rosetta = "type A:\n" +
                "        attr1 string (0..1)\n" +
                "        Attr2 string (0..1)\n";

        String expectedJson = "{\"attr1\":\"foo\",\"Attr2\":\"bar\"}";
        assertJsonSerialisation(rosetta, expectedJson, "com.rosetta.test.model.A");
    }

    @Test
    void testSerialisationWithIdReference() throws JsonProcessingException {
        String rosetta =
                "         type A:\n" +
                        "        a1 string (0..1)\n" +
                        "        a2 B (0..1)      [metadata reference]\n" +
                        " type B: [metadata key]" +
                        "        b1 A (0..1)\n" +
                        "        b2 string (0..1)\n";

        assertJsonSerialisation(rosetta, "{\"a1\":\"foo\",\"a2\":{\"globalReference\":\"XXXXXXXX\"}}", "com.rosetta.test.model.A");
        assertJsonSerialisation(rosetta, "{\"b1\":{\"a1\":\"foo\",\"a2\":{\"globalReference\":\"XXXXXXXX\"}},\"b2\":\"bar\"}", "com.rosetta.test.model.B");
    }

    @Test
    void testReferenceRemoval() throws JsonProcessingException {
        String rosetta =
                "         type Top:\n" +
                        "        party Party (1..1)\n" +
                        "        partyRole PartyRole (1..1)\n" +
                        " type PartyRole:\n" +
                        "        role string (1..1)\n" +
                        "        partyReference Party (1..1) [metadata reference]\n" +
                        " type Party: [metadata key]" +
                        "        partyName string (1..1)\n";

        assertJsonSerialisation(rosetta, "{\"party\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"},\"partyRole\":{\"partyReference\":{\"externalReference\":\"EXT\",\"globalReference\":\"GREF\"},\"role\":\"bar\"}}", "com.rosetta.test.model.Top");
        String inputJson = "{\"party\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"},\"partyRole\":{\"partyReference\":{\"externalReference\":\"EXT\",\"globalReference\":\"GREF\",\"value\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"}},\"role\":\"bar\"}}";
        String expectedJson = "{\"party\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"},\"partyRole\":{\"partyReference\":{\"externalReference\":\"EXT\",\"globalReference\":\"GREF\"},\"role\":\"bar\"}}";
        assertJsonSerialisation(rosetta, inputJson, expectedJson,
                "com.rosetta.test.model.Top");
    }

    @Test
    void testSerialisationWithAddressLocation() throws JsonProcessingException {
        String rosetta =
                "         type ResolvablePriceQuantity:\n" +
                        "        resolvedPrice Price (0..1)  [metadata address \"pointsTo\"=PriceQuantity->price]\n" +
                        " type PriceQuantity:" +
                        "        price Price (0..1)          [metadata location]\n" +
                        " type Price:" +
                        "        rate number (0..1)\n";

        assertJsonSerialisation(rosetta, "{\"resolvedPrice\":{\"address\":{\"scope\":\"DOC\",\"value\":\"price-1\"}}}", "com.rosetta.test.model.ResolvablePriceQuantity");
        assertJsonSerialisation(rosetta,
                "{\"price\":{\"meta\":{\"location\":[{\"scope\":\"DOC\",\"value\":\"price-1\"}]},\"value\":{\"rate\":999}}}",
                "com.rosetta.test.model.PriceQuantity");
    }

    private void assertJsonSerialisation(String rosetta, String expectedJson, String fqClassName) throws JsonProcessingException {
        assertJsonSerialisation(rosetta, expectedJson, expectedJson, fqClassName);
    }

    private void assertJsonSerialisation(String rosetta, String inputJson, String expectedJson, String fqClassName) throws JsonProcessingException {
        HashMap<String, String> generatedCodeMap = codeGeneratorTestHelper.generateCode(rosetta);

        codeGeneratorTestHelper.writeClasses(generatedCodeMap, Thread.currentThread().getStackTrace()[2].getMethodName());

        Map<String, Class<?>> compiledCode = codeGeneratorTestHelper.compileToClasses(generatedCodeMap);

        @SuppressWarnings("unchecked")
        Class<? extends RosettaModelObject> compilesClass = (Class<? extends RosettaModelObject>) compiledCode.get(fqClassName);

        RosettaModelObject value = mapper.readValue(inputJson, compilesClass);
        String actualJson = mapper.writeValueAsString(value);
        assertEquals(expectedJson, actualJson);

        // We need to check to see that the builder class also serialises to the same value
        Class<? extends RosettaModelObjectBuilder> builderClass = compilesClass.getAnnotation(RosettaClass.class).builder();
        RosettaModelObject builderValue = mapper.readValue(inputJson, builderClass);
        String actualBuilderJson = mapper.writeValueAsString(builderValue.toBuilder());
        assertEquals(expectedJson, actualBuilderJson);
    }

}
