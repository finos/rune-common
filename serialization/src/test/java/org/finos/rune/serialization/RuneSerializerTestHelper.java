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
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.regnosys.rosetta.RosettaRuntimeModule;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.config.RuneConfiguration;
import com.regnosys.rosetta.config.RuneGeneratorsConfiguration;
import com.regnosys.rosetta.config.RuneModelConfiguration;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RuneDataType;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.eclipse.xtext.common.TerminalsStandaloneSetup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuneSerializerTestHelper {
    public static final String TEST_MODEL_NAME = "serialization";

    @SuppressWarnings("unchecked")
    public static <T extends RosettaModelObject> Class<T> generateCompileAndGetRootDataType(String namespacePrefix,
                                                                                            String groupName,
                                                                                            List<Path> rosettaPaths,
                                                                                            CodeGeneratorTestHelper helper,
                                                                                            DynamicCompiledClassLoader dynamicCompiledClassLoader) {
        String[] rosettaFileContents = rosettaPaths.stream().map(RuneSerializerTestHelper::readAsString).toArray(String[]::new);
        Map<String, String> generatedCode = helper.generateCode(rosettaFileContents);
        Map<String, Class<?>> compiledCode = helper.compileToClasses(generatedCode);
        dynamicCompiledClassLoader.setCompiledCode(compiledCode);
        String rootClassName = namespacePrefix + groupName + ".Root";
        Class<?> aClass = compiledCode.get(rootClassName);
        if (aClass == null) {
            throw new AssertionError("Unable to locate generated Root type " + rootClassName);
        }
        return (Class<T>) aClass;
    }

    public static CompiledGroup compileGroup(
            String namespacePrefix,
            String groupName,
            List<Path> rosettaPaths,
            CodeGeneratorTestHelper helper,
            DynamicCompiledClassLoader dynamicCompiledClassLoader) {
        String[] rosettaFileContents = rosettaPaths.stream().map(RuneSerializerTestHelper::readAsString).toArray(String[]::new);
        Map<String, String> generatedCode = helper.generateCode(rosettaFileContents);
        Map<String, Class<?>> compiledCode = helper.compileToClasses(generatedCode);
        dynamicCompiledClassLoader.setCompiledCode(compiledCode);
        String rootClassName = namespacePrefix + groupName + ".Root";
        Class<?> rootClass = compiledCode.get(rootClassName);
        if (rootClass == null) {
            throw new AssertionError("Unable to locate generated Root type " + rootClassName);
        }

        Map<String, String> classNamesBySimpleName = new HashMap<>();
        for (String className : compiledCode.keySet()) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot > -1 && lastDot < className.length() - 1) {
                classNamesBySimpleName.put(className.substring(lastDot + 1), className);
            }
        }

        @SuppressWarnings("unchecked")
        Class<RosettaModelObject> rootType = (Class<RosettaModelObject>) rootClass;
        return new CompiledGroup(rootType, classNamesBySimpleName, compiledCode, dynamicCompiledClassLoader);
    }

    public static String readAsString(Path jsonPath) {
        try {
            return new String(Files.readAllBytes(jsonPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> groups(String testType) {
        try (Stream<Path> files = Files.list(Paths.get("src/test/resources/" + testType))) {
            return files.filter(Files::isDirectory).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path getGroupPath(String testType, String groupName) {
        return Paths.get("src/test/resources").resolve(testType).resolve(groupName);
    }

    public static Path getFile(Path groupPath, String fileName) {
        try (Stream<Path> files = Files.list(groupPath)) {
            return files.filter(x -> x.getFileName().toString().equals(fileName)).collect(Collectors.toList())
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> listFiles(Path groupPath, String suffix) {
        try (Stream<Path> files = Files.list(groupPath)) {
            return files.filter(x -> x.getFileName().toString().endsWith(suffix)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ObjectMapper newObjectMapper(ClassLoader classLoader) {
        ObjectMapper objectMapper = new RuneJsonObjectMapper();
        objectMapper.setTypeFactory(objectMapper.getTypeFactory().withClassLoader(classLoader));
        return objectMapper;
    }

    public static <T extends RosettaModelObject> T fromJson(ObjectMapper objectMapper, String runeJson, Class<T> type) {
        try {
            return objectMapper.readValue(runeJson, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(ObjectMapper objectMapper, RosettaModelObject runeObject) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(runeObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static RosettaModelObjectBuilder newBuilder(Class<?> type) throws IOException {
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

    public static RosettaModelObject build(Object builder) throws IOException {
        try {
            return (RosettaModelObject) builder.getClass().getMethod("build").invoke(builder);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Unable to build Rosetta object from " + builder.getClass().getName(), e);
        }
    }

    public static void invokeSetter(Object target, String setterName, Object value) throws IOException {
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

    public static Object invokeGetter(Object target, String getterName) {
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke " + getterName + " on " + target.getClass().getName(), e);
        }
    }

    public static Object findNestedGetterResult(Object target, String... getterNames) {
        Object current = target;
        for (String getterName : getterNames) {
            if (current == null) {
                return null;
            }
            current = invokeGetter(current, getterName);
        }
        return current;
    }

    public static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean isPrimitiveCompatible(Class<?> primitiveType, Object value) {
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

    public static Injector setupInjector() {
        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
        TerminalsStandaloneSetup.doSetup();

        Module module = Modules.override(new RosettaRuntimeModule()).with(new AbstractModule() {
            @Override
            protected void configure() {
                bind(RuneConfiguration.class).toInstance(new RuneConfiguration(
                        new RuneModelConfiguration(TEST_MODEL_NAME, Collections.emptyList()),
                        new ArrayList<>(),
                        new RuneGeneratorsConfiguration()
                ));
            }
        });
        Injector injector = Guice.createInjector(module);
        rosettaStandaloneSetup.register(injector);
        return injector;
    }

    public static class CompiledGroup {
        private final Class<RosettaModelObject> rootType;
        private final Map<String, String> classNamesBySimpleName;
        private final Map<String, Class<?>> compiledCode;
        private final DynamicCompiledClassLoader dynamicCompiledClassLoader;

        private CompiledGroup(
                Class<RosettaModelObject> rootType,
                Map<String, String> classNamesBySimpleName,
                Map<String, Class<?>> compiledCode,
                DynamicCompiledClassLoader dynamicCompiledClassLoader) {
            this.rootType = rootType;
            this.classNamesBySimpleName = classNamesBySimpleName;
            this.compiledCode = compiledCode;
            this.dynamicCompiledClassLoader = dynamicCompiledClassLoader;
        }

        public Class<RosettaModelObject> getRootType() {
            return rootType;
        }

        public Map<String, Class<?>> getCompiledCode() {
            return compiledCode;
        }

        public Class<?> getType(String simpleName) throws IOException {
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
