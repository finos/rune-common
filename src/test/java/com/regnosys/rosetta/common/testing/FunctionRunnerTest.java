package com.regnosys.rosetta.common.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class FunctionRunnerTest {

    private static Path SCCACHE_PATH;

    @Test
    void runTestFunc1UsingRunner() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {

        ObjectMapper objectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

        Optional<ExecutionDescriptor> executionDescriptor = ExecutionDescriptor.loadExecutionDescriptor(objectMapper,
                Resources.getResource("function-runner-test/execution-descriptor-1.json")).stream()
                .filter(x -> x.getGroup().equals("group-1"))
                .filter(x -> x.getName().equals("test-1"))
                .findFirst();

        if (!executionDescriptor.isPresent()) {
            fail("Could not read find executionDescriptor for group-1:test-1");
        }

        FunctionRunner functionRunner = new FunctionRunner(executionDescriptor.get(),
                this::getInstance,
                this.getClass().getClassLoader(),
                objectMapper, Paths.get(""));
        FunctionRunner.FunctionRunnerResult<Object, Object> run = functionRunner.run();


        if (!run.isSuccess()) {
            assertEquals(run.getJsonExpected(), run.getJsonActual());
        }
    }
    @Test
    void runTestFunc1UsingRunnerScCache() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, URISyntaxException {

        createTempSCCacheFolder();
        ObjectMapper objectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        Optional<ExecutionDescriptor> executionDescriptor = ExecutionDescriptor.loadExecutionDescriptor(objectMapper,
                        Resources.getResource("function-runner-test-2/execution-descriptor-2.json")).stream()
                .filter(x -> x.getGroup().equals("group-1"))
                .filter(x -> x.getName().equals("test-1"))
                .findFirst();

        if (!executionDescriptor.isPresent()) {
            fail("Could not read find executionDescriptor for group-1:test-1");
        }

        FunctionRunner functionRunner = new FunctionRunner(executionDescriptor.get(),
                this::getInstance,
                this.getClass().getClassLoader(),
                objectMapper, SCCACHE_PATH.resolve("MODEL-ID"));
        FunctionRunner.FunctionRunnerResult<Object, Object> run = functionRunner.run();

        if (!run.isSuccess()) {
            assertEquals(run.getJsonExpected(), run.getJsonActual());
        }
    }

    private <T> T getInstance(Class<T> x) {
        try {
            return x.getConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestFunc1Input {
        private String testInput;

        public TestFunc1Input() {
        }

        public TestFunc1Input(String testInput) {
            this.testInput = testInput;
        }

        public String getTestInput() {
            return testInput;
        }
    }

    public static class TestFunc1Output {
        private String testOutput;

        public TestFunc1Output() {
        }

        public TestFunc1Output(String testOutput) {
            this.testOutput = testOutput;
        }

        public String getTestOutput() {
            return testOutput;
        }
    }

    public static class TestFunc1 implements ExecutableFunction<TestFunc1Input, TestFunc1Output> {

        @Override
        public TestFunc1Output execute(TestFunc1Input o) {
            return new TestFunc1Output("bar");
        }

        @Override
        public Class<TestFunc1Input> getInputType() {
            return TestFunc1Input.class;
        }

        @Override
        public Class<TestFunc1Output> getOutputType() {
            return TestFunc1Output.class;

        }
    }
    private void createTempSCCacheFolder() throws IOException, URISyntaxException {
        SCCACHE_PATH = Files.createTempDirectory("scCache");
        Path sourcePath = Paths.get("function-runner-test-2");

        File source = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(sourcePath.toString())).toURI());
        Path target = SCCACHE_PATH.resolve(sourcePath.toString());
        FileUtils.copyDirectory(source, target.toFile());
    }

}