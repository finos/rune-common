package com.regnosys.rosetta.common.serialisation.json;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.mixin.RosettaJSONModule;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaDataType;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaSerialisationTest {

    @Inject
    CodeGeneratorTestHelper codeGeneratorTestHelper;

    @Test
    void testBasicSerialisation() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();

        String rosetta = "type A:\n" +
                "        attr1 string (0..1)\n" +
                "        attr2 string (0..1)\n";

        String expectedJson = "{\"attr1\":\"foo\",\"attr2\":\"bar\"}";
        assertJsonSerialisation(mapper,  rosetta, expectedJson, "com.rosetta.test.model.A");
    }

    @Test
    void testBasicSerialisationMultiCard() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        String rosetta = "type A:\n" +
                "        attr1 string (0..*)\n" +
                "        attr2 string (0..*)\n";

        String expectedJson = "{\"attr1\":[\"foo1\",\"foo2\"],\"attr2\":[\"bar\"]}";
        assertJsonSerialisation(mapper, rosetta, expectedJson, "com.rosetta.test.model.A");
    }

    @Test
    void testBasicEnumWithNativeEnumSupport() throws JsonProcessingException {
        ObjectMapper mapper = new RosettaObjectMapperCreator(true, new RosettaJSONModule(true), new ObjectMapper()).create();

        String rosetta = "type Top:\n" +
                "          aSingle A (0..1)\n" +
                "          aMulti A (0..*)\n" +
                "         enum A:\n" +
                "          a1\n" +
                "          A2\n" +
                "          a_3\n" +
                "          A4 displayName \"---A---4---\"";

        String legacyJson =   "{\"aSingle\":\"A_1\",\"aMulti\":[\"A_1\",\"A2\",\"A_3\",\"---A---4---\"]}";
        String expectedJson = "{\"aSingle\":\"a1\",\"aMulti\":[\"a1\",\"A2\",\"a_3\",\"---A---4---\"]}";

        assertJsonSerialisation(mapper, rosetta, legacyJson, expectedJson, "com.rosetta.test.model.Top");

        assertJsonSerialisation(mapper, rosetta, expectedJson, "com.rosetta.test.model.Top");
    }

    @Test
    void testBasicEnumWithJavaEnumSupport() throws JsonProcessingException {
        ObjectMapper mapper = new RosettaObjectMapperCreator(false, new RosettaJSONModule(false), new ObjectMapper()).create();

        String rosetta = "type Top:\n" +
                "          aSingle A (0..1)\n" +
                "          aMulti A (0..*)\n" +
                "         enum A:\n" +
                "          a1\n" +
                "          A2\n" +
                "          a_3\n" +
                "          A4 displayName \"A-4\"";

        String expectedJson =   "{\"aSingle\":\"A_1\",\"aMulti\":[\"A_1\",\"A2\",\"A_3\",\"A-4\"]}";
        assertJsonSerialisation(mapper, rosetta, expectedJson, "com.rosetta.test.model.Top");
    }

    @Test
    void testSerialisationWithUpperCaseAttribute() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        String rosetta = "type A:\n" +
                "        attr1 string (0..1)\n" +
                "        Attr2 string (0..1)\n";

        String expectedJson = "{\"attr1\":\"foo\",\"Attr2\":\"bar\"}";
        assertJsonSerialisation(mapper, rosetta, expectedJson, "com.rosetta.test.model.A");
    }

    @Test
    void testSerialisationWithIdReference() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        String rosetta =
                "         type A:\n" +
                        "        a1 string (0..1)\n" +
                        "        a2 B (0..1)      [metadata reference]\n" +
                        " type B: [metadata key]" +
                        "        b1 A (0..1)\n" +
                        "        b2 string (0..1)\n";

        assertJsonSerialisation(mapper, rosetta, "{\"a1\":\"foo\",\"a2\":{\"globalReference\":\"XXXXXXXX\"}}", "com.rosetta.test.model.A");
        assertJsonSerialisation(mapper, rosetta, "{\"b1\":{\"a1\":\"foo\",\"a2\":{\"globalReference\":\"XXXXXXXX\"}},\"b2\":\"bar\"}", "com.rosetta.test.model.B");
    }

    @Test
    void testReferenceRemoval() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        String rosetta =
                "         type Top:\n" +
                        "        party Party (1..1)\n" +
                        "        partyRole PartyRole (1..1)\n" +
                        " type PartyRole:\n" +
                        "        role string (1..1)\n" +
                        "        partyReference Party (1..1) [metadata reference]\n" +
                        " type Party: [metadata key]" +
                        "        partyName string (1..1)\n";

        assertJsonSerialisation(mapper, rosetta, "{\"party\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"},\"partyRole\":{\"partyReference\":{\"externalReference\":\"EXT\",\"globalReference\":\"GREF\"},\"role\":\"bar\"}}", "com.rosetta.test.model.Top");
        String inputJson = "{\"party\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"},\"partyRole\":{\"partyReference\":{\"externalReference\":\"EXT\",\"globalReference\":\"GREF\",\"value\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"}},\"role\":\"bar\"}}";
        String expectedJson = "{\"party\":{\"meta\":{\"externalKey\":\"EXT\",\"globalKey\":\"GREF\"},\"partyName\":\"foo\"},\"partyRole\":{\"partyReference\":{\"externalReference\":\"EXT\",\"globalReference\":\"GREF\"},\"role\":\"bar\"}}";
        assertJsonSerialisation(mapper, rosetta, inputJson, expectedJson,
                "com.rosetta.test.model.Top");
    }

    @Test
    void testSerialisationWithAddressLocation() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        String rosetta =
                "         type ResolvablePriceQuantity:\n" +
                        "        resolvedPrice Price (0..1)  [metadata address \"pointsTo\"=PriceQuantity->price]\n" +
                        " type PriceQuantity:" +
                        "        price Price (0..1)          [metadata location]\n" +
                        " type Price:" +
                        "        rate number (0..1)\n";

        assertJsonSerialisation(mapper, rosetta, "{\"resolvedPrice\":{\"address\":{\"scope\":\"DOC\",\"value\":\"price-1\"}}}", "com.rosetta.test.model.ResolvablePriceQuantity");
        assertJsonSerialisation(mapper, rosetta,
                "{\"price\":{\"meta\":{\"location\":[{\"scope\":\"DOC\",\"value\":\"price-1\"}]},\"value\":{\"rate\":999}}}",
                "com.rosetta.test.model.PriceQuantity");
    }

    private void assertJsonSerialisation(ObjectMapper mapper, String rosetta, String expectedJson, String fqClassName) throws JsonProcessingException {
        assertJsonSerialisation(mapper, rosetta, expectedJson, expectedJson, fqClassName);
    }

    private void assertJsonSerialisation(ObjectMapper mapper, String rosetta, String inputJson, String expectedJson, String fqClassName) throws JsonProcessingException {
        HashMap<String, String> generatedCodeMap = codeGeneratorTestHelper.generateCode(rosetta);

        // Uncomment so see the generated code in target/<test-name>
        //codeGeneratorTestHelper.writeClasses(generatedCodeMap, Thread.currentThread().getStackTrace()[3].getMethodName());

        Map<String, Class<?>> compiledCode = codeGeneratorTestHelper.compileToClasses(generatedCodeMap);

        @SuppressWarnings("unchecked")
        Class<? extends RosettaModelObject> compilesClass = (Class<? extends RosettaModelObject>) compiledCode.get(fqClassName);

        RosettaModelObject value = mapper.readValue(inputJson, compilesClass);
        String actualJson = mapper.writeValueAsString(value);
        assertJsonEquals(expectedJson, actualJson);

        // We need to check to see that the builder class also serialises to the same value
        Class<? extends RosettaModelObjectBuilder> builderClass = compilesClass.getAnnotation(RosettaDataType.class).builder();
        RosettaModelObject builderValue = mapper.readValue(inputJson, builderClass);
        String actualBuilderJson = mapper.writeValueAsString(builderValue.toBuilder());
        assertJsonEquals(expectedJson, actualBuilderJson);
    }

    private void assertJsonEquals(String expectedJson, String actualJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        TreeMap<String, ?> expectedJsonMap = mapper.readValue(expectedJson, TreeMap.class);
        TreeMap<String, ?> actualJsonMap = mapper.readValue(actualJson, TreeMap.class);
        assertEquals(expectedJsonMap, actualJsonMap);
    }
}
