package org.finos.rune.serialization;

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
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RuneSerializerKeyPruningTest {
    public static final String TEST_TYPE = "rune-serializer-key-pruning-test";
    public static final String NAMESPACE_PREFIX = TEST_MODEL_NAME + ".test.passing.";
    private static DynamicCompiledClassLoader dynamicCompiledClassLoader;
    private ObjectMapper objectMapper;

    private static CodeGeneratorTestHelper helper;

    @BeforeAll
    static void beforeAll() {
        Injector injector = setupInjector();
        helper = injector.getInstance(CodeGeneratorTestHelper.class);
    }

    @BeforeEach
    void setUp() {
        objectMapper = new RuneJsonObjectMapper();
        dynamicCompiledClassLoader = new DynamicCompiledClassLoader();
        objectMapper.setTypeFactory(objectMapper.getTypeFactory().withClassLoader(dynamicCompiledClassLoader));
    }

    @Test
    void testMetaKeyOnObjectWithNoReferenceToItIsPruned() {
        Path groupPath = getGroupPath("metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-without-ref-input.json"));

        RosettaModelObject deserializedObject = fromJson(input, rootDataType);
        String result = toJson(deserializedObject);

        String expected = readAsString(getFile(groupPath, "node-key-without-ref-expected.json"));

        assertEquals(expected, result);
    }

    @Test
    void testMetaKeyOnObjectWithReferenceIsNotPruned() {
        Path groupPath = getGroupPath("metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-with-ref.json"));

        RosettaModelObject deserializedObject = fromJson(input, rootDataType);
        String result = toJson(deserializedObject);

        assertEquals(input, result);
    }

    @Test
    void testMetaIdOnAttributeWithNoReferenceToItIsPruned() {
        Path groupPath = getGroupPath("metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "attribute-key-without-ref-input.json"));

        RosettaModelObject deserializedObject = fromJson(input, rootDataType);
        String result = toJson(deserializedObject);

        String expected = readAsString(getFile(groupPath, "attribute-key-without-ref-expected.json"));

        assertEquals(expected, result);
    }

    @Test
    void testMetaIdOnAttributeWithReferenceIsNotPruned() {
        Path groupPath = getGroupPath("metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "attribute-key-with-ref.json"));

        RosettaModelObject deserializedObject = fromJson(input, rootDataType);
        String result = toJson(deserializedObject);

        assertEquals(input, result);
    }

    @Disabled
    @Test
    void testDuplicateReferenceGlobalIsPruned() {
        Path groupPath = getGroupPath("metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-with-duplicate-ref-input.json"));

        RosettaModelObject deserializedObject = fromJson(input, rootDataType);
        String result = toJson(deserializedObject);

        String expected = readAsString(getFile(groupPath, "node-key-with-duplicate-ref-expected.json"));

        assertEquals(expected, result);
    }

    private Class<RosettaModelObject> getRootRosettaModelObjectClass(Path groupPath, String fileName) {
        Path rosetta = getFile(groupPath, fileName);
        String groupName = groupPath.getFileName().toString();
        return generateCompileAndGetRootDataType(NAMESPACE_PREFIX, groupName, Collections.singletonList(rosetta), helper, dynamicCompiledClassLoader);
    }

    private Path getGroupPath(String groupName) {
        return  Paths.get("src/test/resources").resolve(TEST_TYPE).resolve(groupName);
    }

    private <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type) {
        try {
            return objectMapper.readValue(runeJson, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends RosettaModelObject> String toJson(T runeObject) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(runeObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
