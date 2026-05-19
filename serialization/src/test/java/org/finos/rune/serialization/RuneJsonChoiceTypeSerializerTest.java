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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RuneDataType;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.rune.serialization.RuneSerializerTestHelper.listFiles;
import static org.finos.rune.serialization.RuneSerializerTestHelper.setupInjector;

public class RuneJsonChoiceTypeSerializerTest {
    private static final String TEST_TYPE = "rune-json-choice-type-serializer-test";
    private static final Map<Path, CompiledGroup> COMPILED_GROUPS = new HashMap<>();

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
    void shouldFailWhenDirectChoiceRuntimeTypeIsNotADeclaredOption() throws IOException {
        CompiledGroup group = getCompiledGroup(getGroupPath("invalidruntimesubtype"));

        RosettaModelObjectBuilder extABuilder = newBuilder(group.getType("ExtA"));
        invokeSetter(extABuilder, "setFieldA", "foo");
        invokeSetter(extABuilder, "setFieldExt", "bar");
        RosettaModelObject extA = build(extABuilder);

        RosettaModelObjectBuilder choiceDataBuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataBuilder, "setA", extA);
        RosettaModelObject choiceData = build(choiceDataBuilder);

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.rootType);
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
        CompiledGroup group = getCompiledGroup(getGroupPath("invalidruntimesubtype"));

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

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.rootType);
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
        CompiledGroup group = getCompiledGroup(getGroupPath("extension"));

        RosettaModelObjectBuilder aBuilder = newBuilder(group.getType("A"));
        invokeSetter(aBuilder, "setFieldA", "alpha");
        RosettaModelObject a = build(aBuilder);

        RosettaModelObjectBuilder choiceDataABuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataABuilder, "setA", a);
        RosettaModelObject choiceDataA = build(choiceDataABuilder);

        RosettaModelObjectBuilder rootABuilder = newBuilder(group.rootType);
        invokeSetter(rootABuilder, "setChoiceData", choiceDataA);
        RosettaModelObject rootA = build(rootABuilder);

        JsonNode choiceDataAJson = objectMapper.readTree(toJson(rootA)).get("choiceData");
        Assertions.assertNotNull(choiceDataAJson);
        Assertions.assertEquals("serialization.test.passing.extensionserializer.A", choiceDataAJson.get("@type").asText());
        Assertions.assertEquals("alpha", choiceDataAJson.get("fieldA").asText());

        RosettaModelObjectBuilder bBuilder = newBuilder(group.getType("B"));
        invokeSetter(bBuilder, "setFieldB", "bravo");
        RosettaModelObject b = build(bBuilder);

        RosettaModelObjectBuilder choiceDataBBuilder = newBuilder(group.getType("ChoiceData"));
        invokeSetter(choiceDataBBuilder, "setB", b);
        RosettaModelObject choiceDataB = build(choiceDataBBuilder);

        RosettaModelObjectBuilder rootBBuilder = newBuilder(group.rootType);
        invokeSetter(rootBBuilder, "setChoiceData", choiceDataB);
        RosettaModelObject rootB = build(rootBBuilder);

        JsonNode choiceDataBJson = objectMapper.readTree(toJson(rootB)).get("choiceData");
        Assertions.assertNotNull(choiceDataBJson);
        Assertions.assertEquals("serialization.test.passing.extensionserializer.B", choiceDataBJson.get("@type").asText());
        Assertions.assertEquals("bravo", choiceDataBJson.get("fieldB").asText());
    }

    @Test
    void shouldSerializeDeepNestedChoiceWithDeclaredConcreteType() throws IOException {
        CompiledGroup group = getCompiledGroup(getGroupPath("extension"));

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

        RosettaModelObjectBuilder rootBuilder = newBuilder(group.rootType);
        invokeSetter(rootBuilder, "setChoiceDeepNested", choiceDeepNested);
        RosettaModelObject root = build(rootBuilder);

        JsonNode choiceDeepNestedJson = objectMapper.readTree(toJson(root)).get("choiceDeepNested");
        Assertions.assertNotNull(choiceDeepNestedJson);
        Assertions.assertEquals("serialization.test.passing.extensionserializer.ExtA", choiceDeepNestedJson.get("@type").asText());
        Assertions.assertEquals("foo", choiceDeepNestedJson.get("fieldA").asText());
        Assertions.assertEquals("bar", choiceDeepNestedJson.get("fieldExt").asText());
    }

    private static Path getGroupPath(String groupName) {
        return Paths.get("src/test/resources").resolve(TEST_TYPE).resolve(groupName);
    }

    private CompiledGroup getCompiledGroup(Path groupPath) {
        CompiledGroup existing = COMPILED_GROUPS.get(groupPath);
        if (existing != null) {
            dynamicCompiledClassLoader.setCompiledCode(existing.compiledCode);
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

        Map<String, String> classNamesBySimpleName = new HashMap<>();
        for (String className : compiledCode.keySet()) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot > -1 && lastDot < className.length() - 1) {
                classNamesBySimpleName.put(className.substring(lastDot + 1), className);
            }
        }

        try {
            @SuppressWarnings("unchecked")
            Class<RosettaModelObject> rootType = (Class<RosettaModelObject>) dynamicCompiledClassLoader.loadClass(rootClassName);
            CompiledGroup compiledGroup = new CompiledGroup(rootType, classNamesBySimpleName, compiledCode);
            COMPILED_GROUPS.put(groupPath, compiledGroup);
            return compiledGroup;
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Unable to load generated types for " + groupPath, e);
        }
    }

    private RosettaModelObjectBuilder newBuilder(Class<?> type) throws IOException {
        RuneDataType runeDataType = type.getAnnotation(RuneDataType.class);
        if (runeDataType == null) {
            throw new IOException("Unable to find Rune data type metadata for " + type.getName());
        }
        try {
            return runeDataType.builder().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IOException("Unable to create builder for " + type.getName(), e);
        }
    }

    private RosettaModelObject build(Object builder) throws IOException {
        try {
            return (RosettaModelObject) builder.getClass().getMethod("build").invoke(builder);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Unable to build Rosetta object from " + builder.getClass().getName(), e);
        }
    }

    private void invokeSetter(Object target, String setterName, Object value) throws IOException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(setterName) || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (value == null || parameterType.isInstance(value) || (parameterType.isPrimitive() && isPrimitiveCompatible(parameterType, value))) {
                try {
                    method.invoke(target, value);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IOException("Unable to invoke " + setterName + " on " + target.getClass().getName(), e);
                }
            }
        }
        throw new IOException("Unable to find compatible setter " + setterName + " on " + target.getClass().getName());
    }

    private boolean isPrimitiveCompatible(Class<?> primitiveType, Object value) {
        if (primitiveType == int.class) {
            return value instanceof Integer;
        }
        if (primitiveType == long.class) {
            return value instanceof Long;
        }
        if (primitiveType == boolean.class) {
            return value instanceof Boolean;
        }
        if (primitiveType == double.class) {
            return value instanceof Double;
        }
        if (primitiveType == float.class) {
            return value instanceof Float;
        }
        if (primitiveType == short.class) {
            return value instanceof Short;
        }
        if (primitiveType == byte.class) {
            return value instanceof Byte;
        }
        if (primitiveType == char.class) {
            return value instanceof Character;
        }
        return false;
    }

    private String toJson(RosettaModelObject runeObject) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(runeObject);
    }

    private static class CompiledGroup {
        private final Class<RosettaModelObject> rootType;
        private final Map<String, String> classNamesBySimpleName;
        private final Map<String, Class<?>> compiledCode;

        private CompiledGroup(Class<RosettaModelObject> rootType, Map<String, String> classNamesBySimpleName, Map<String, Class<?>> compiledCode) {
            this.rootType = rootType;
            this.classNamesBySimpleName = classNamesBySimpleName;
            this.compiledCode = compiledCode;
        }

        private Class<?> getType(String simpleName) throws IOException {
            String className = classNamesBySimpleName.get(simpleName);
            if (className == null) {
                throw new IOException("Unable to resolve generated class for " + simpleName);
            }
            try {
                return dynamicCompiledClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IOException("Unable to load generated class " + className, e);
            }
        }
    }
}
