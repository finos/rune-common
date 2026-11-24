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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Each expected file under {@code rune-json-attributes-only-test/<group>} is the attributes-only output for
 * the Rune JSON file of the same name in {@code rune-serializer-round-trip-test/<group>}, whose model it uses.
 */
class RuneJsonAttributesOnlyTest {
    private static final String TEST_TYPE = "rune-json-attributes-only-test";
    private static final String NAMESPACE_PREFIX = TEST_MODEL_NAME + ".test.passing.";

    private static final Map<String, Class<RosettaModelObject>> ROOT_TYPES = new HashMap<>();
    private static DynamicCompiledClassLoader dynamicCompiledClassLoader;
    private static CodeGeneratorTestHelper helper;

    @BeforeAll
    static void beforeAll() {
        Injector injector = setupInjector();
        helper = injector.getInstance(CodeGeneratorTestHelper.class);
        dynamicCompiledClassLoader = new DynamicCompiledClassLoader();
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("testCases")
    void writesOnlyModelAttributes(String group, String testCaseName, RosettaModelObject model, String expectedJson) {
        String actual = toJson(RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader), model);

        assertEquals(expectedJson, actual, testCaseName);
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("testCases")
    void writesNoMetaPropertyThroughObjectMapper(String group, String testCaseName, RosettaModelObject model, String expectedJson) throws Exception {
        ObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);

        JsonNode actual = mapper.readTree(mapper.writeValueAsString(model));

        assertEquals(mapper.readTree(expectedJson), actual, testCaseName);
    }

    @Test
    void refusesToReadModelObject() {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);

        InvalidDefinitionException error = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{}", RosettaModelObject.class));
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void refusesToReadModelObjectThroughObjectReader() {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);

        InvalidDefinitionException error = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readerFor(RosettaModelObject.class).readValue("{}"));
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void refusesToReadListOfModelObjects() {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);

        InvalidDefinitionException error = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("[]", new TypeReference<List<RosettaModelObject>>() {}));
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void refusesToConvertToModelObject() {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);
        Class<RosettaModelObject> rootType = compiledBasicRootType();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mapper.convertValue(Collections.emptyMap(), rootType));
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void refusesToReadModelObjectsAsSequence() {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);
        Class<RosettaModelObject> rootType = compiledBasicRootType();

        InvalidDefinitionException error = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readerFor(rootType).readValues("{} {}").next());
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void writesEmptiedRootListAsEmptyArray() throws Exception {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly();

        String json = mapper.writeValueAsString(Collections.singletonList(Collections.singletonMap("@ref", "a")));

        assertEquals("[]", json);
    }

    @Test
    void wrapsRootNameOnce() throws Exception {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly();

        String json = mapper.writer().withRootName("trade").writeValueAsString(Collections.singletonMap("x", 1));

        assertEquals("{\"trade\":{\"x\":1}}", json);
    }

    @Test
    void writesToGeneratorWithoutCodec() throws Exception {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly();
        StringWriter out = new StringWriter();

        try (JsonGenerator generator = new JsonFactory().createGenerator(out)) {
            mapper.writeValue(generator, Collections.singletonMap("amount", new BigDecimal("1.50")));
        }

        assertEquals("{\"amount\":1.50}", out.toString());
    }

    @Test
    void writesFloatingPointNumbersAsStandardMapperDoes() throws Exception {
        Map<String, Object> numbers = new LinkedHashMap<>();
        numbers.put("float", 0.1f);
        numbers.put("double", 0.1d);
        numbers.put("decimal", new BigDecimal("1.50"));

        assertEquals(new RuneJsonObjectMapper().writeValueAsString(numbers),
                RuneJsonObjectMapper.attributesOnly().writeValueAsString(numbers));
    }

    @Test
    void readsJsonTree() throws Exception {
        RuneJsonObjectMapper mapper = RuneJsonObjectMapper.attributesOnly(dynamicCompiledClassLoader);

        assertEquals("bar", mapper.readTree("{\"foo\":\"bar\"}").get("foo").asText());
    }

    @Test
    void reportsWhetherAttributesOnly() {
        assertTrue(RuneJsonObjectMapper.attributesOnly().isAttributesOnly());
        assertFalse(new RuneJsonObjectMapper().isAttributesOnly());
        assertFalse(new RuneJsonObjectMapper(dynamicCompiledClassLoader).isAttributesOnly());
    }

    private static Class<RosettaModelObject> compiledBasicRootType() {
        return generateCompileAndGetRootDataType(NAMESPACE_PREFIX, "basic",
                listFiles(getGroupPath(RuneJsonSerializerRoundTripTest.TEST_TYPE, "basic"), ".rosetta"),
                helper, new DynamicCompiledClassLoader());
    }

    static Stream<Arguments> testCases() {
        return groups(TEST_TYPE).stream()
                .flatMap(expectedGroupPath -> {
                    String groupName = expectedGroupPath.getFileName().toString();
                    Path inputGroupPath = getGroupPath(RuneJsonSerializerRoundTripTest.TEST_TYPE, groupName);
                    Class<RosettaModelObject> rootType = ROOT_TYPES.computeIfAbsent(groupName,
                            name -> generateCompileAndGetRootDataType(NAMESPACE_PREFIX, name,
                                    listFiles(inputGroupPath, ".rosetta"), helper, dynamicCompiledClassLoader));
                    ObjectMapper reader = newObjectMapper(dynamicCompiledClassLoader);

                    return listFiles(expectedGroupPath, ".json").stream()
                            .map(expectedPath -> {
                                String fileName = expectedPath.getFileName().toString();
                                String inputJson = readAsString(getFile(inputGroupPath, fileName));
                                return Arguments.of(groupName, fileName,
                                        fromJson(reader, inputJson, rootType), readAsString(expectedPath));
                            });
                });
    }
}
