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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;

public class RuneJsonChoiceTypeDeserializerTest {
    private static final String TEST_TYPE = "rune-json-choice-type-deserializer-test";
    private static final String PASSING_NAMESPACE_PREFIX = "serialization.test.passing.";
    private static final String FAILING_NAMESPACE_PREFIX = "serialization.test.failing.";
    private static final Set<String> FAILING_GROUPS = new HashSet<>(Arrays.asList("invalidchoiceoption", "invalidnestedchoiceoption"));
    private static final Map<Path, RuneSerializerTestHelper.CompiledGroup> COMPILED_GROUPS = new HashMap<>();
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
        objectMapper = newObjectMapper(dynamicCompiledClassLoader);
    }

    @Test
    void shouldDeserializeChoiceDataUsingExactTypeFromTypeMetadata() throws JsonProcessingException {
        Path groupPath = getGroupPath(TEST_TYPE, "extension");
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
        Path groupPath = getGroupPath(TEST_TYPE, "extension");
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
    void shouldDeserializeChoiceDataWhenFailingOnUnknownProperties() throws JsonProcessingException {
        Path groupPath = getGroupPath(TEST_TYPE, "extension");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = withoutModelAndVersion(objectMapper, readAsString(getFile(groupPath, "choice-data-extension.json")));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        RosettaModelObject deserializedObject = fromJson(json, rootDataType);

        Object extA = invokeGetter(invokeGetter(deserializedObject, "getChoiceData"), "getExtA");
        Assertions.assertNotNull(extA, "ChoiceData should deserialize @type ExtA into ExtA");
        Assertions.assertEquals("bar", invokeGetter(extA, "getFieldExt"));
    }

    @Test
    void shouldDeserializeNestedChoiceDataWhenFailingOnUnknownProperties() throws JsonProcessingException {
        Path groupPath = getGroupPath(TEST_TYPE, "extension");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = withoutModelAndVersion(objectMapper, readAsString(getFile(groupPath, "choice-deep-nested-extended.json")));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        RosettaModelObject deserializedObject = fromJson(json, rootDataType);

        Object choiceDeepNested = invokeGetter(deserializedObject, "getChoiceDeepNested");
        Object choiceData = firstNonNull(
                findNestedGetterResult(choiceDeepNested, "getMiddleChoiceA", "getChoiceData"),
                findNestedGetterResult(choiceDeepNested, "getMiddleChoiceB", "getChoiceData")
        );
        Assertions.assertNotNull(choiceData, "ChoiceDeepNested should resolve to a nested ChoiceData option");
        Assertions.assertNotNull(invokeGetter(choiceData, "getExtA"), "Nested ChoiceData should deserialize @type ExtA into ExtA");
    }

    @Test
    void shouldFailOnUnknownChoiceOptionFieldWhenUnknownPropertiesAreRejected() throws JsonProcessingException {
        Path groupPath = getGroupPath(TEST_TYPE, "extension");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = withoutModelAndVersion(objectMapper, readAsString(getFile(groupPath, "choice-data-unknown-field.json")));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        UnrecognizedPropertyException exception = Assertions.assertThrows(UnrecognizedPropertyException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertEquals("notAField", exception.getPropertyName());
    }

    @Test
    void shouldFailWhenChoiceDoesNotDeclareTypeFromTypeMetadata() {
        Path groupPath = getGroupPath(TEST_TYPE, "invalidchoiceoption");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "invalid-choice-option.json"));

        MismatchedInputException exception = Assertions.assertThrows(MismatchedInputException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertTrue(exception.getMessage().contains("Unable to resolve Rune choice option"));
    }

    @Test
    void shouldFailWhenNestedChoiceDoesNotDeclareTypeFromTypeMetadata() {
        Path groupPath = getGroupPath(TEST_TYPE, "invalidnestedchoiceoption");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "invalid-nested-choice-option.json"));

        MismatchedInputException exception = Assertions.assertThrows(MismatchedInputException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertTrue(exception.getMessage().contains("Unable to resolve Rune choice option"));
    }

    @Test
    void shouldFailToReadHeaderWrittenWithTheJavaPackageName() {
        Path groupPath = getGroupPath(TEST_TYPE, "new");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "java-name-header.json"));

        InvalidTypeIdException exception = Assertions.assertThrows(InvalidTypeIdException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertEquals("serialization.test.passing._new.Root", exception.getTypeId());
    }

    @Test
    void shouldFailToReadSubtypeWrittenWithTheJavaPackageName() {
        Path groupPath = getGroupPath(TEST_TYPE, "new");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "java-name-subtype.json"));

        InvalidTypeIdException exception = Assertions.assertThrows(InvalidTypeIdException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertEquals("serialization.test.passing._new.B", exception.getTypeId());
    }

    @Test
    void shouldReportUnknownSubtypeByItsRuneName() {
        Path groupPath = getGroupPath(TEST_TYPE, "new");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "unknown-subtype.json"));

        InvalidTypeIdException exception = Assertions.assertThrows(InvalidTypeIdException.class,
                () -> fromJson(json, rootDataType));

        Assertions.assertEquals("serialization.test.passing.new.Missing", exception.getTypeId());
        Assertions.assertTrue(exception.getMessage().contains("'serialization.test.passing.new.Missing'"), exception.getMessage());
    }

    private Class<RosettaModelObject> getRootRosettaModelObjectClass(Path groupPath) {
        RuneSerializerTestHelper.CompiledGroup existing = COMPILED_GROUPS.get(groupPath);
        if (existing != null) {
            dynamicCompiledClassLoader.setCompiledCode(existing.getCompiledCode());
            return existing.getRootType();
        }

        String groupName = groupPath.getFileName().toString();
        String namespacePrefix = FAILING_GROUPS.contains(groupName)
                ? FAILING_NAMESPACE_PREFIX
                : PASSING_NAMESPACE_PREFIX;
        RuneSerializerTestHelper.CompiledGroup compiledGroup = compileGroup(
                namespacePrefix,
                groupName,
                listFiles(groupPath, ".rosetta"),
                helper,
                dynamicCompiledClassLoader
        );
        COMPILED_GROUPS.put(groupPath, compiledGroup);
        return compiledGroup.getRootType();
    }

    private <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(runeJson, type);
    }
}
