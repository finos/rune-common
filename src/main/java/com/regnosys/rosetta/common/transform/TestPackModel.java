package com.regnosys.rosetta.common.transform;

import java.util.List;

public record TestPackModel(String id, String pipelineId, String name, List<SampleModel> samples) {
    public record SampleModel(String id, String name, String inputPath, String outputPath, String outputTabulatedPath, Assertions assertions) {
        public record Assertions(Integer modelValidationFailures, Boolean schemaValidationFailure, Boolean runtimeError) {}
    }
}
