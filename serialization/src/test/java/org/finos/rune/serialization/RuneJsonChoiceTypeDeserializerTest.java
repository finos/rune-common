package org.finos.rune.serialization;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.rune.serialization.RuneSerializerTestHelper.getFile;
import static org.finos.rune.serialization.RuneSerializerTestHelper.listFiles;
import static org.finos.rune.serialization.RuneSerializerTestHelper.readAsString;
import static org.finos.rune.serialization.RuneSerializerTestHelper.setupInjector;

public class RuneJsonChoiceTypeDeserializerTest {
    private static final String TEST_TYPE = "rune-json-choice-type-deserializer-test";
    private static final Map<Path, Class<RosettaModelObject>> ROOT_TYPES = new HashMap<>();
    private static DynamicCompiledClassLoader dynamicCompiledClassLoader;
    private static CodeGeneratorTestHelper helper;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        Injector injector = setupInjector();
        helper = injector.getInstance(CodeGeneratorTestHelper.class);
        dynamicCompiledClassLoader = new DynamicCompiledClassLoader();
    }

    @BeforeEach
    void setUp() {
        objectMapper = new RuneJsonObjectMapper();
        objectMapper.setTypeFactory(objectMapper.getTypeFactory().withClassLoader(dynamicCompiledClassLoader));
    }

    @Test
    void shouldDeserializeChoiceDataUsingExactTypeFromTypeMetadata() throws JsonProcessingException {
        Path groupPath = getGroupPath("extension");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "choice-data-extension.json"));

        RosettaModelObject deserializedObject = fromJson(json, rootDataType);

        Object choiceData = invokeGetter(deserializedObject, "getChoiceData");
        Assertions.assertNotNull(choiceData);

        Object a = invokeGetter(choiceData, "getA");
        Object extA = invokeGetter(choiceData, "getExtA");

        Assertions.assertNull(a, "ChoiceData should not deserialize @type ExtA into A");
        Assertions.assertNotNull(extA, "ChoiceData should deserialize @type ExtA into ExtA");
        Assertions.assertEquals("foo", invokeGetter(extA, "getFieldA"));
        Assertions.assertEquals("bar", invokeGetter(extA, "getFieldExt"));
    }

    @Test
    void shouldDeserializeNestedChoiceDataUsingExactTypeFromTypeMetadata() throws JsonProcessingException {
        Path groupPath = getGroupPath("extension");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "choice-deep-nested-extended.json"));

        RosettaModelObject deserializedObject = fromJson(json, rootDataType);

        Object choiceDeepNested = invokeGetter(deserializedObject, "getChoiceDeepNested");
        Assertions.assertNotNull(choiceDeepNested);

        Object choiceData = firstNonNull(
                findNestedGetterResult(choiceDeepNested, "getMiddleChoiceA", "getChoiceData"),
                findNestedGetterResult(choiceDeepNested, "getMiddleChoiceB", "getChoiceData")
        );
        Assertions.assertNotNull(choiceData, "ChoiceDeepNested should resolve to a nested ChoiceData option");

        Object a = invokeGetter(choiceData, "getA");
        Object extA = invokeGetter(choiceData, "getExtA");

        Assertions.assertNull(a, "Nested ChoiceData should not deserialize @type ExtA into A");
        Assertions.assertNotNull(extA, "Nested ChoiceData should deserialize @type ExtA into ExtA");
        Assertions.assertEquals("foo", invokeGetter(extA, "getFieldA"));
        Assertions.assertEquals("bar", invokeGetter(extA, "getFieldExt"));
    }

    @Test
    void shouldFailWhenChoiceDoesNotDeclareTypeFromTypeMetadata() {
        Path groupPath = getGroupPath("invalidchoiceoption");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "invalid-choice-option.json"));

        MismatchedInputException exception = Assertions.assertThrows(MismatchedInputException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertTrue(exception.getMessage().contains("Unable to resolve Rune choice option"));
    }

    @Test
    void shouldFailWhenNestedChoiceDoesNotDeclareTypeFromTypeMetadata() {
        Path groupPath = getGroupPath("invalidnestedchoiceoption");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "invalid-nested-choice-option.json"));

        MismatchedInputException exception = Assertions.assertThrows(MismatchedInputException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertTrue(exception.getMessage().contains("Unable to resolve Rune choice option"));
    }

    private static Path getGroupPath(String groupName) {
        return Paths.get("src/test/resources").resolve(TEST_TYPE).resolve(groupName);
    }

    @SuppressWarnings("unchecked")
    private Class<RosettaModelObject> getRootRosettaModelObjectClass(Path groupPath) {
        Class<RosettaModelObject> existing = ROOT_TYPES.get(groupPath);
        if (existing != null) {
            return existing;
        }

        List<Path> rosettas = listFiles(groupPath, ".rosetta");
        String[] rosettaFileContents = rosettas.stream().map(RuneSerializerTestHelper::readAsString).toArray(String[]::new);
        Map<String, String> generatedCode = helper.generateCode(rosettaFileContents);
        Map<String, Class<?>> compiledCode = helper.compileToClasses(generatedCode);
        dynamicCompiledClassLoader.setCompiledCode(compiledCode);
        String rootClassName = compiledCode.keySet().stream()
                .filter(className -> className.endsWith(".Root"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Unable to locate generated Root type for " + groupPath));

        try {
            Class<RosettaModelObject> rootType = (Class<RosettaModelObject>) dynamicCompiledClassLoader.loadClass(rootClassName);
            ROOT_TYPES.put(groupPath, rootType);
            return rootType;
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Unable to load generated Root type " + rootClassName, e);
        }
    }

    private <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(runeJson, type);
    }

    private Object invokeGetter(Object target, String getterName) {
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke " + getterName + " on " + target.getClass().getName(), e);
        }
    }

    private Object findNestedGetterResult(Object target, String... getterNames) {
        Object current = target;
        for (String getterName : getterNames) {
            if (current == null) {
                return null;
            }
            current = invokeGetter(current, getterName);
        }
        return current;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
