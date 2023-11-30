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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaCSourceCompilerTest {
    private static final String TEST_RESOURCES = "compile-test";

    private Path input;
    private Path output;
    private JavaCancellableCompiler javaCancellableCompiler;

    @BeforeEach
    void setup() throws IOException {
        input = Files.createTempDirectory("JavaCSourceCompilerTest-Input");
        output = Files.createTempDirectory("JavaCSourceCompilerTest-Output");
    }

    @Test
    void compilesHelloWorld() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        javaCancellableCompiler = new JavaCSourceCancellableCompiler(Executors.newSingleThreadExecutor(), true, false, false, JavaCompileReleaseFlag.JAVA_11);
        String helloWorldJava = "HelloWorld.java";
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList(helloWorldJava));
        JavaCompilationResult compilationResult = javaCancellableCompiler.compile(sourceJavas, output, () -> false);

        assertThat(compilationResult.isCompilationComplete(), is(true));
        assertThat(compilationResult.isCompilationSuccessful(), is(true));
        File classFile = output.resolve("HelloWorld.class").toFile();
        assertThat(classFile.exists(), is(true));
    }

    @Test
    void respectsDeleteOnErrorWhenFlagWhenTrue() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        javaCancellableCompiler = new JavaCSourceCancellableCompiler(Executors.newSingleThreadExecutor(), true, true, false, JavaCompileReleaseFlag.JAVA_11);
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList("HelloWorld.java", "BrokenHelloWorld.java"));
        JavaCompilationResult compilationResult = javaCancellableCompiler.compile(sourceJavas, output, () -> false);

        assertThat(compilationResult.isCompilationComplete(), is(true));
        assertThat(compilationResult.isCompilationSuccessful(), is(false));

        try(Stream<Path> list = Files.list(output)) {
            assertThat(list.count(), equalTo(0L));
        }
    }

    @Test
    void respectsDeleteOnErrorWhenFlagWhenFalse() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        javaCancellableCompiler = new JavaCSourceCancellableCompiler(Executors.newSingleThreadExecutor(), true, false, false, JavaCompileReleaseFlag.JAVA_11);
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList("HelloWorld.java", "BrokenHelloWorld.java"));
        JavaCompilationResult compilationResult = javaCancellableCompiler.compile(sourceJavas, output, () -> false);

        assertThat(compilationResult.isCompilationComplete(), is(true));
        assertThat(compilationResult.isCompilationSuccessful(), is(false));

        try(Stream<Path> list = Files.list(output)) {
            assertThat(list.count(), equalTo(1L));
        }
        File goodFile = output.resolve("HelloWorld.class").toFile();
        assertThat(goodFile.exists(), is(true));
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