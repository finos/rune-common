package com.regnosys.rosetta.common.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.hashing.ReferenceResolverConfig;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class FunctionRunnerTest {

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
                objectMapper,
                ReferenceResolverConfig.noScopeOrExcludedPaths());
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
}