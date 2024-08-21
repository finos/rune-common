package com.regnosys.rosetta.common.serialisation.json;

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
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.ReferenceService;
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
public class ProxySerialisationTest {

    @Inject
    CodeGeneratorTestHelper codeGeneratorTestHelper;
    @Inject
    ReferenceService referenceService;

    @Test
    void testProxySerialisation() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();

        String rosetta = "type A:\n" +
                "        attr1 string (0..1)\n" +
                "        attr2 string (0..1)\n";

        String expectedJson = "{\"attr1\":\"foo\",\"attr2\":\"bar\"}";

        HashMap<String, String> generatedCode = codeGeneratorTestHelper.generateCode(rosetta);
        Map<String, Class<?>> compiledCode = codeGeneratorTestHelper.compileToClasses(generatedCode);

        Class<?> compiledClass = compiledCode.get("com.rosetta.test.model.A");

        Object value = mapper.readValue(expectedJson, compiledClass);
        Object proxy = referenceService.register(value, "key", (Class<Object>)compiledClass);

        String actualJson = mapper.writeValueAsString(proxy);
        assertJsonEquals(expectedJson, actualJson);
    }

    private void assertJsonEquals(String expectedJson, String actualJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        TreeMap<String, ?> expectedJsonMap = mapper.readValue(expectedJson, TreeMap.class);
        TreeMap<String, ?> actualJsonMap = mapper.readValue(actualJson, TreeMap.class);
        assertEquals(expectedJsonMap, actualJsonMap);
    }
}
