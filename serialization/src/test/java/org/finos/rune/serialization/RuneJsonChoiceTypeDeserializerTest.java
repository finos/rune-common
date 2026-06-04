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
import com.rosetta.model.metafields.MetaFields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

public class RuneJsonChoiceTypeDeserializerTest {
    private static final String TEST_TYPE = "rune-json-choice-type-deserializer-test";
    private static final String PASSING_NAMESPACE_PREFIX = "serialization.test.passing.";
    private static final String FAILING_NAMESPACE_PREFIX = "serialization.test.failing.";
    private static final Set<String> FAILING_GROUPS = new HashSet<>(Arrays.asList("invalidchoiceoption", "invalidnestedchoiceoption"));
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
        objectMapper = newObjectMapper(dynamicCompiledClassLoader);
    }

    @Test
    void shouldDeserializeChoiceDataWithMetadataKey() throws JsonProcessingException {
        Path groupPath = getGroupPath(TEST_TYPE, "metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "choice-data-metakey.json"));

        RosettaModelObject deserializedObject = fromJson(json, rootDataType);

        Object choiceData = invokeGetter(deserializedObject, "getChoiceData");
        Assertions.assertNotNull(choiceData);

        Object a = invokeGetter(choiceData, "getA");
        Assertions.assertEquals("foo", invokeGetter(a, "getFieldA"));

        Object meta = invokeGetter(choiceData, "getMeta");
        Assertions.assertNotNull(meta);
        assertInstanceOf(MetaFields.class, meta);
        MetaFields metaFields = (MetaFields) meta;
        assertEquals("someExternalKey", metaFields.getExternalKey());
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
    void shouldFailWhenChoiceDoesNotDeclareTypeFromTypeMetadata() {
        Path groupPath = getGroupPath(TEST_TYPE, "invalidchoiceoption");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "invalid-choice-option.json"));

        MismatchedInputException exception = Assertions.assertThrows(MismatchedInputException.class,
                () -> fromJson(json, rootDataType));

        assertTrue(exception.getMessage().contains("Unable to resolve Rune choice option"));
    }

    @Test
    void shouldFailWhenNestedChoiceDoesNotDeclareTypeFromTypeMetadata() {
        Path groupPath = getGroupPath(TEST_TYPE, "invalidnestedchoiceoption");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath);
        String json = readAsString(getFile(groupPath, "invalid-nested-choice-option.json"));

        MismatchedInputException exception = Assertions.assertThrows(MismatchedInputException.class,
                () -> fromJson(json, rootDataType));

        assertTrue(exception.getMessage().contains("Unable to resolve Rune choice option"));
    }

    private Class<RosettaModelObject> getRootRosettaModelObjectClass(Path groupPath) {
        Class<RosettaModelObject> existing = ROOT_TYPES.get(groupPath);
        if (existing != null) {
            return existing;
        }

        String groupName = groupPath.getFileName().toString();
        String namespacePrefix = FAILING_GROUPS.contains(groupName)
                ? FAILING_NAMESPACE_PREFIX
                : PASSING_NAMESPACE_PREFIX;
        List<Path> rosettas = new ArrayList<>(listFiles(groupPath, ".rosetta"));

        Class<RosettaModelObject> rootType = generateCompileAndGetRootDataType(
                namespacePrefix,
                groupName,
                rosettas,
                helper,
                dynamicCompiledClassLoader
        );
        ROOT_TYPES.put(groupPath, rootType);
        return rootType;
    }

    private <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(runeJson, type);
    }
}
