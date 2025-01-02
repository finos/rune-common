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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.regnosys.rosetta.RosettaRuntimeModule;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.config.RosettaConfiguration;
import com.regnosys.rosetta.config.RosettaGeneratorsConfiguration;
import com.regnosys.rosetta.config.RosettaModelConfiguration;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.eclipse.xtext.common.TerminalsStandaloneSetup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuneSerializerTestHelper {

    public static final String TEST_MODEL_NAME = "serialization";

    @SuppressWarnings("unchecked")
    public static <T extends RosettaModelObject> Class<T> generateCompileAndGetRootDataType(String groupName,
                                                                                            List<Path> rosettaPaths,
                                                                                            CodeGeneratorTestHelper helper, DynamicCompiledClassLoader dynamicCompiledClassLoader) {
        String[] rosettaFileContents = rosettaPaths.stream().map(RuneSerializerTestHelper::readAsString).toArray(String[]::new);
        HashMap<String, String> generatedCode = helper.generateCode(rosettaFileContents);
        Map<String, Class<?>> compiledCode = helper.compileToClasses(generatedCode);
        dynamicCompiledClassLoader.setCompiledCode(compiledCode);
        Class<?> aClass = compiledCode.get(TEST_MODEL_NAME + ".test." + groupName + ".Root");
        return (Class<T>) aClass;
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

    public static Injector setupInjector() {
        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
        TerminalsStandaloneSetup.doSetup();

        Module module = Modules.override(new RosettaRuntimeModule()).with(new AbstractModule() {
            @Override
            protected void configure() {
                bind(RosettaConfiguration.class).toInstance(new RosettaConfiguration(
                        new RosettaModelConfiguration(TEST_MODEL_NAME),
                        new ArrayList<>(),
                        new RosettaGeneratorsConfiguration()
                ));
            }
        });
        Injector injector = Guice.createInjector(module);
        rosettaStandaloneSetup.register(injector);
        return injector;
    }
}
