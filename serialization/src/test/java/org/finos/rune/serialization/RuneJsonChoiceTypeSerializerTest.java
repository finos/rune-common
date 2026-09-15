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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.metafields.MetaFields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;

public class RuneJsonChoiceTypeSerializerTest {
    private static final String TEST_TYPE = "rune-json-choice-type-serializer-test";
    private static final String PASSING_NAMESPACE_PREFIX = "serialization.test.passing.";
    private static final String FAILING_NAMESPACE_PREFIX = "serialization.test.failing.";
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
    void shouldSerializeSameChoiceTypeInListWhenNotBuiltInWrapper() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "list"));

        RosettaModelObjectBuilder aBuilder = newBuilder(group.getType("A"));
        invokeSetter(aBuilder, "setFieldA", "foo");
        RosettaModelObjectBuilder choiceDataABuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataABuilder, "setA", aBuilder);

        RosettaModelObjectBuilder a2Builder = newBuilder(group.getType("A"));
        invokeSetter(a2Builder, "setFieldA", "foo2");
        RosettaModelObjectBuilder choiceDataA2Builder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataA2Builder, "setA", a2Builder);

        RosettaModelObjectBuilder economicTermsBuilder = newBuilder(group.getType("EconomicTerms"));
        invokeSetter(economicTermsBuilder, "setPayout", Arrays.asList(choiceDataABuilder, choiceDataA2Builder));

        RosettaModelObjectBuilder productBuilder = newBuilder(group.getType("Product"));
        invokeSetter(productBuilder, "setEconomicTerms", economicTermsBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setProduct", productBuilder);
        NonRosettaWrapper wrapperWithBuilders = new NonRosettaWrapper(rootBuilder);

        String builtJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapperWithBuilders);
        JsonNode payoutJson = objectMapper.readTree(builtJson)
                .path("root")
                .path("product")
                .path("economicTerms")
                .path("payout");

        Assertions.assertTrue(payoutJson.isArray());
        Assertions.assertEquals(2, payoutJson.size());
        Assertions.assertEquals("foo", payoutJson.get(0).path("fieldA").asText());
        Assertions.assertEquals("foo2", payoutJson.get(1).path("fieldA").asText());
    }

    @Test
    void shouldSerializeSameChoiceTypeInListWhenNotBuiltWithoutWrapper() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "list"));

        RosettaModelObjectBuilder aBuilder = newBuilder(group.getType("A"));
        invokeSetter(aBuilder, "setFieldA", "foo");
        RosettaModelObjectBuilder choiceDataABuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataABuilder, "setA", aBuilder);

        RosettaModelObjectBuilder a2Builder = newBuilder(group.getType("A"));
        invokeSetter(a2Builder, "setFieldA", "foo2");
        RosettaModelObjectBuilder choiceDataA2Builder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataA2Builder, "setA", a2Builder);

        RosettaModelObjectBuilder economicTermsBuilder = newBuilder(group.getType("EconomicTerms"));
        invokeSetter(economicTermsBuilder, "setPayout", Arrays.asList(choiceDataABuilder, choiceDataA2Builder));

        RosettaModelObjectBuilder productBuilder = newBuilder(group.getType("Product"));
        invokeSetter(productBuilder, "setEconomicTerms", economicTermsBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setProduct", productBuilder);

        String builtJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootBuilder);
        JsonNode payoutJson = objectMapper.readTree(builtJson)
                .path("product")
                .path("economicTerms")
                .path("payout");

        Assertions.assertTrue(payoutJson.isArray());
        Assertions.assertEquals(2, payoutJson.size());
        Assertions.assertEquals("foo", payoutJson.get(0).path("fieldA").asText());
        Assertions.assertEquals("foo2", payoutJson.get(1).path("fieldA").asText());
    }

    @Test
    void shouldSerializeSameChoiceTypeInListWhenBuiltInWrapper() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "list"));

        RosettaModelObjectBuilder aBuilder = newBuilder(group.getType("A"));
        invokeSetter(aBuilder, "setFieldA", "foo");
        RosettaModelObjectBuilder choiceDataABuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataABuilder, "setA", aBuilder);

        RosettaModelObjectBuilder a2Builder = newBuilder(group.getType("A"));
        invokeSetter(a2Builder, "setFieldA", "foo2");
        RosettaModelObjectBuilder choiceDataA2Builder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataA2Builder, "setA", a2Builder);

        RosettaModelObjectBuilder economicTermsBuilder = newBuilder(group.getType("EconomicTerms"));
        invokeSetter(economicTermsBuilder, "setPayout", Arrays.asList(choiceDataABuilder, choiceDataA2Builder));

        RosettaModelObjectBuilder productBuilder = newBuilder(group.getType("Product"));
        invokeSetter(productBuilder, "setEconomicTerms", economicTermsBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setProduct", productBuilder);

        RosettaModelObject builtRoot = build(rootBuilder);
        NonRosettaWrapper wrapperWithBuiltObject = new NonRosettaWrapper(builtRoot);
        String builtJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapperWithBuiltObject);
        JsonNode payoutJson = objectMapper.readTree(builtJson)
                .path("root")
                .path("product")
                .path("economicTerms")
                .path("payout");

        Assertions.assertTrue(payoutJson.isArray());
        Assertions.assertEquals(2, payoutJson.size());
        Assertions.assertEquals("foo", payoutJson.get(0).path("fieldA").asText());
        Assertions.assertEquals("foo2", payoutJson.get(1).path("fieldA").asText());
    }

    @Test
    void shouldSerializeChoiceWithMetadataKey() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "metakey"));

        RosettaModelObjectBuilder aBuilder = newBuilder(group.getType("A"));
        invokeSetter(aBuilder, "setFieldA", "foo");
        MetaFields someGlobalKey = MetaFields.builder().setExternalKey("someExternalKey").build();
        invokeSetter(aBuilder, "setMeta", someGlobalKey);
        RosettaModelObject a = build(aBuilder);

        RosettaModelObjectBuilder choiceDataABuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataABuilder, "setA", a);
        RosettaModelObject choiceDataA = build(choiceDataABuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setChoiceData", choiceDataA);
        RosettaModelObject root = build(rootBuilder);

        JsonNode choiceDataJson = objectMapper.readTree(toJson(root)).get("choiceData");
        Assertions.assertNotNull(choiceDataJson);
        Assertions.assertEquals("serialization.test.passing.metakey.A", choiceDataJson.get("@type").asText());
        Assertions.assertEquals("foo", choiceDataJson.get("fieldA").asText());
    }

    @Test
    void shouldFailWhenDirectChoiceRuntimeTypeIsNotADeclaredOption() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "invalidruntimesubtype"));

        RosettaModelObjectBuilder extABuilder = newBuilder(group.getType("ExtA"));
        invokeSetter(extABuilder, "setFieldA", "foo");
        invokeSetter(extABuilder, "setFieldExt", "bar");
        RosettaModelObject extA = build(extABuilder);

        RosettaModelObjectBuilder choiceDataBuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataBuilder, "setA", extA);
        RosettaModelObject choiceData = build(choiceDataBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setChoiceData", choiceData);
        RosettaModelObject root = build(rootBuilder);

        JsonMappingException exception = Assertions.assertThrows(
                JsonMappingException.class,
                () -> toJson(root)
        );
        Assertions.assertTrue(exception.getMessage().contains("runtime type must exactly match the declared choice option type"));
    }

    @Test
    void shouldFailWhenNestedChoiceRuntimeTypeIsNotADeclaredOption() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "invalidruntimesubtype"));

        RosettaModelObjectBuilder extABuilder = newBuilder(group.getType("ExtA"));
        invokeSetter(extABuilder, "setFieldA", "foo");
        invokeSetter(extABuilder, "setFieldExt", "bar");
        RosettaModelObject extA = build(extABuilder);

        RosettaModelObjectBuilder choiceDataBuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataBuilder, "setA", extA);
        RosettaModelObject choiceData = build(choiceDataBuilder);

        RosettaModelObjectBuilder middleChoiceABuilder = newBuilder(group.getType("MiddleChoiceA"));
        invokeSetter(middleChoiceABuilder, "setChoiceData", choiceData);
        RosettaModelObject middleChoiceA = build(middleChoiceABuilder);

        RosettaModelObjectBuilder choiceDeepNestedBuilder = newBuilder(group.getType("ChoiceDeepNested"));
        invokeSetter(choiceDeepNestedBuilder, "setMiddleChoiceA", middleChoiceA);
        RosettaModelObject choiceDeepNested = build(choiceDeepNestedBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setChoiceDeepNested", choiceDeepNested);
        RosettaModelObject root = build(rootBuilder);

        JsonMappingException exception = Assertions.assertThrows(
                JsonMappingException.class,
                () -> toJson(root)
        );
        Assertions.assertTrue(exception.getMessage().contains("runtime type must exactly match the declared choice option type"));
    }

    @Test
    void shouldSerializeDirectChoiceWithDeclaredConcreteTypes() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "extension"));

        RosettaModelObjectBuilder aBuilder = newBuilder(group.getType("A"));
        invokeSetter(aBuilder, "setFieldA", "alpha");
        RosettaModelObject a = build(aBuilder);

        RosettaModelObjectBuilder choiceDataABuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataABuilder, "setA", a);
        RosettaModelObject choiceDataA = build(choiceDataABuilder);

        RosettaModelObjectBuilder rootABuilder = newBuilder(group.getRootType());
        invokeSetter(rootABuilder, "setChoiceData", choiceDataA);
        RosettaModelObject rootA = build(rootABuilder);

        JsonNode choiceDataAJson = objectMapper.readTree(toJson(rootA)).get("choiceData");
        Assertions.assertNotNull(choiceDataAJson);
        Assertions.assertEquals("serialization.test.passing.extension.A", choiceDataAJson.get("@type").asText());
        Assertions.assertEquals("alpha", choiceDataAJson.get("fieldA").asText());

        RosettaModelObjectBuilder bBuilder = newBuilder(group.getType("B"));
        invokeSetter(bBuilder, "setFieldB", "bravo");
        RosettaModelObject b = build(bBuilder);

        RosettaModelObjectBuilder choiceDataBBuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataBBuilder, "setB", b);
        RosettaModelObject choiceDataB = build(choiceDataBBuilder);

        RosettaModelObjectBuilder rootBBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBBuilder, "setChoiceData", choiceDataB);
        RosettaModelObject rootB = build(rootBBuilder);

        JsonNode choiceDataBJson = objectMapper.readTree(toJson(rootB)).get("choiceData");
        Assertions.assertNotNull(choiceDataBJson);
        Assertions.assertEquals("serialization.test.passing.extension.B", choiceDataBJson.get("@type").asText());
        Assertions.assertEquals("bravo", choiceDataBJson.get("fieldB").asText());
    }

    @Test
    void shouldSerializeDeepNestedChoiceWithDeclaredConcreteType() throws IOException {
        RuneSerializerTestHelper.CompiledGroup group = getCompiledGroup(getGroupPath(TEST_TYPE, "extension"));

        RosettaModelObjectBuilder extABuilder = newBuilder(group.getType("ExtA"));
        invokeSetter(extABuilder, "setFieldA", "foo");
        invokeSetter(extABuilder, "setFieldExt", "bar");
        RosettaModelObject extA = build(extABuilder);

        RosettaModelObjectBuilder choiceDataBuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataBuilder, "setExtA", extA);
        RosettaModelObject choiceData = build(choiceDataBuilder);

        RosettaModelObjectBuilder middleChoiceABuilder = newBuilder(group.getType("MiddleChoiceA"));
        invokeSetter(middleChoiceABuilder, "setChoiceData", choiceData);
        RosettaModelObject middleChoiceA = build(middleChoiceABuilder);

        RosettaModelObjectBuilder choiceDeepNestedBuilder = newBuilder(group.getType("ChoiceDeepNested"));
        invokeSetter(choiceDeepNestedBuilder, "setMiddleChoiceA", middleChoiceA);
        RosettaModelObject choiceDeepNested = build(choiceDeepNestedBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.getRootType());
        invokeSetter(rootBuilder, "setChoiceDeepNested", choiceDeepNested);
        RosettaModelObject root = build(rootBuilder);

        JsonNode choiceDeepNestedJson = objectMapper.readTree(toJson(root)).get("choiceDeepNested");
        Assertions.assertNotNull(choiceDeepNestedJson);
        Assertions.assertEquals("serialization.test.passing.extension.ExtA", choiceDeepNestedJson.get("@type").asText());
        Assertions.assertEquals("foo", choiceDeepNestedJson.get("fieldA").asText());
        Assertions.assertEquals("bar", choiceDeepNestedJson.get("fieldExt").asText());
    }

    private RuneSerializerTestHelper.CompiledGroup getCompiledGroup(Path groupPath) {
        RuneSerializerTestHelper.CompiledGroup existing = COMPILED_GROUPS.get(groupPath);
        if (existing != null) {
            dynamicCompiledClassLoader.setCompiledCode(existing.getCompiledCode());
            return existing;
        }

        String groupName = groupPath.getFileName().toString();
        String namespacePrefix = "invalidruntimesubtype".equals(groupName)
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
        return compiledGroup;
    }

    private String toJson(RosettaModelObject runeObject) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(runeObject);
    }

    private static class NonRosettaWrapper {
        @JsonProperty
        private final RosettaModelObject root;

        private NonRosettaWrapper(RosettaModelObject root) {
            this.root = root;
        }

        public RosettaModelObject getRoot() {
            return root;
        }
    }

}
