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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RuneSerializerPruningTest {
    public static final String TEST_TYPE = "rune-serializer-pruning-test";
    public static final String NAMESPACE_PREFIX = TEST_MODEL_NAME + ".test.passing.";
    public static final String GROUP_META_KEY = "metakey";
    public static final String GROUP_OBJECT_PRUNING = "objectprune";
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
        dynamicCompiledClassLoader = new DynamicCompiledClassLoader();
        objectMapper = newObjectMapper(dynamicCompiledClassLoader);
    }

    @Test
    void testStringWithEmptyMetaPrunesOnDeserialise() throws NoSuchFieldException, IllegalAccessException {
        Path groupPath = getGroupPath(TEST_TYPE, GROUP_OBJECT_PRUNING);
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "object-prune.rosetta");
        String input = readAsString(getFile(groupPath, "string-with-empty-meta.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);

        Object fieldA = getFieldValue(deserializedObject, "fieldA");
        Object meta = getFieldValue(fieldA, "meta");
        assertNull(meta);
    }

    @Test
    void testMetaKeyOnObjectWithNoReferenceToItIsPruned() {
        Path groupPath = getGroupPath(TEST_TYPE, "metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-without-ref-input.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);
        String result = toJson(objectMapper, deserializedObject);

        String expected = readAsString(getFile(groupPath, "node-key-without-ref-expected.json"));

        assertEquals(expected, result);
    }

    @Test
    void testMetaKeyOnObjectWithReferenceIsNotPruned() {
        Path groupPath = getGroupPath(TEST_TYPE, "metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-with-ref.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);
        String result = toJson(objectMapper, deserializedObject);

        assertEquals(input, result);
    }

    @Test
    void testMetaIdOnAttributeWithNoReferenceToItIsPruned() {
        Path groupPath = getGroupPath(TEST_TYPE, "metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "attribute-key-without-ref-input.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);
        String result = toJson(objectMapper, deserializedObject);

        String expected = readAsString(getFile(groupPath, "attribute-key-without-ref-expected.json"));

        assertEquals(expected, result);
    }

    @Test
    void testMetaIdOnAttributeWithReferenceIsNotPruned() {
        Path groupPath = getGroupPath(TEST_TYPE, "metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-key.rosetta");
        String input = readAsString(getFile(groupPath, "attribute-key-with-ref.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);
        String result = toJson(objectMapper, deserializedObject);

        assertEquals(input, result);
    }

    @Test
    void testDuplicateReferencesArePruned() {
        Path groupPath = getGroupPath(TEST_TYPE, "metakey");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-duplicate-refs.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-with-duplicate-ref-input.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);
        String result = toJson(objectMapper, deserializedObject);

        String expected = readAsString(getFile(groupPath, "node-key-with-duplicate-ref-expected.json"));

        assertEquals(expected, result);
    }

    /*
     * TODO:
     * This test fails as the @key does not get pruned. This is because the GlobalKeyPruningStrategy is not able to see the top level
     * key as instead of being of type GlobalKey it is an instance of GlobalFieldKeys.
     */
    @Disabled
    @Test
    void testRedundantGlobalKeyIsPrunedFromRootObject() {
        Path groupPath = getGroupPath(TEST_TYPE, GROUP_META_KEY);
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "meta-root-key.rosetta");
        String input = readAsString(getFile(groupPath, "node-key-with-redundant-key-input.json"));

        RosettaModelObject deserializedObject = fromJson(objectMapper, input, rootDataType);
        String result = toJson(objectMapper, deserializedObject);

        String expected = readAsString(getFile(groupPath, "node-key-with-redundant-key-expected.json"));

        assertEquals(expected, result);
    }

    private Class<RosettaModelObject> getRootRosettaModelObjectClass(Path groupPath, String fileName) {
        Path rosetta = getFile(groupPath, fileName);
        String groupName = groupPath.getFileName().toString();
        return generateCompileAndGetRootDataType(NAMESPACE_PREFIX, groupName, Collections.singletonList(rosetta), helper, dynamicCompiledClassLoader);
    }

    public static Object getFieldValue(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
