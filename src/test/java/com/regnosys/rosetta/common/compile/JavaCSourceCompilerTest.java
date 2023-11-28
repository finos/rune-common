package com.regnosys.rosetta.common.compile;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaCSourceCompilerTest {
    private static final String TEST_RESOURCES = "compile-test";

    private Path input;
    private Path output;
    private JavaCompiler javaCompiler;

    @BeforeEach
    void setup() throws IOException {
        input = Files.createTempDirectory("JavaCSourceCompilerTest-Input");
        output = Files.createTempDirectory("JavaCSourceCompilerTest-Output");
        javaCompiler = new JavaCSourceCompiler(Executors.newSingleThreadExecutor(), true, true, "11");
    }

    @Test
    void compilesHelloWorld() throws IOException {
        String helloWorldJava = "HelloWorld.java";
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList(helloWorldJava));
        javaCompiler.compile(sourceJavas, output, () -> false);
        File classFile = output.resolve(helloWorldJava).toFile();
        assertTrue(classFile.exists());
    }

    List<Path> setupSourceJavas(List<String> javaFiles) throws IOException {
        ArrayList<Path> javaSourcePaths = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        for (String javaFile : javaFiles) {
            File file = new File(Objects.requireNonNull(classLoader.getResource(String.format("%s/%s", TEST_RESOURCES, javaFile))).getFile());
            Path target = input.resolve(javaFile);
            Files.copy(file.toPath(), target);
            javaSourcePaths.add(target);
        }
        return javaSourcePaths;
    }
}