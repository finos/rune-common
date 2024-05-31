package com.regnosys.rosetta.common.compile;

/*-
 * ==============
 * Rosetta Common
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

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JavaCSourceCancellableCompilerTest {
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

        assertThat(compilationResult.isCompilationSuccessful(), is(true));
        File classFile = output.resolve("HelloWorld.class").toFile();
        assertThat(classFile.exists(), is(true));
    }

    @Test
    void respectsDeleteOnErrorWhenFlagWhenTrue() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        javaCancellableCompiler = new JavaCSourceCancellableCompiler(Executors.newSingleThreadExecutor(), true, true, false, JavaCompileReleaseFlag.JAVA_11);
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList("HelloWorld.java", "BrokenHelloWorld.java"));
        JavaCompilationResult compilationResult = javaCancellableCompiler.compile(sourceJavas, output, () -> false);

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

        assertThat(compilationResult.isCompilationSuccessful(), is(false));

        try(Stream<Path> list = Files.list(output)) {
            assertThat(list.count(), equalTo(1L));
        }
        File goodFile = output.resolve("HelloWorld.class").toFile();
        assertThat(goodFile.exists(), is(true));
    }

    @Test
    void compileCancelsTask() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        ExecutorService mockExecutor = mock(ExecutorService.class);
        CompletableFuture<Boolean> compilationTask = new CompletableFuture<>();
        when(mockExecutor.submit(any(JavaCompiler.CompilationTask.class)))
                .thenReturn(compilationTask);

        javaCancellableCompiler = new JavaCSourceCancellableCompiler(5,
                500,
                mockExecutor,
                true,
                false,
                false,
                JavaCompileReleaseFlag.JAVA_11);


        String helloWorldJava = "HelloWorld.java";
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList(helloWorldJava));

        AtomicInteger cancelCheckCount = new AtomicInteger(0);
        CancelIndicator cancelIndicator = () -> {
            if (cancelCheckCount.get() == 2) {
                return true;
            }
            cancelCheckCount.getAndIncrement();
            return false;
        };

        JavaCompilationResult compilationResult = javaCancellableCompiler.compile(sourceJavas, output, cancelIndicator);

        assertThat(compilationResult.getCompilationCompletionState(), equalTo(CompilationCompletionState.NOT_COMPLETE));
        assertThat(compilationTask.isCancelled(), is(true));
    }

    @Test
    void compileTimesOutCorrectly() throws IOException {
        ExecutorService mockExecutor = mock(ExecutorService.class);
        CompletableFuture<Boolean> compilationTask = new CompletableFuture<>();
        when(mockExecutor.submit(any(JavaCompiler.CompilationTask.class)))
                .thenReturn(compilationTask);

        javaCancellableCompiler = new JavaCSourceCancellableCompiler(1,
                5,
                mockExecutor,
                true,
                false,
                false,
                JavaCompileReleaseFlag.JAVA_11);


        String helloWorldJava = "HelloWorld.java";
        List<Path> sourceJavas = setupSourceJavas(Lists.newArrayList(helloWorldJava));

        CancelIndicator cancelIndicator = () -> false;

        assertThrows(TimeoutException.class, () -> {
            javaCancellableCompiler.compile(sourceJavas, output, cancelIndicator);
        });
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
