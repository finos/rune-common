package com.regnosys.rosetta.common.transform;

import java.util.List;

public class TestPackModel {
    
    private String id;
    
    private String pipelineId;
    private String name;
    private List<SampleModel> samples;

    public TestPackModel(String id, String pipelineId, String name, List<SampleModel> samples) {
        this.id = id;
        this.pipelineId = pipelineId;
        this.name = name;
        this.samples = samples;
    }

    public String getId() {
        return id;
    }

    public String getPipelineId() {
        return pipelineId;
    }
    public String getName() {
        return name;
    }
    public List<SampleModel> getSamples() {
        return samples;
    }

    public class SampleModel {

        private String id;
        private String name;
        String inputPath;
        private String outputPath;
        private String outputTabulatedPath;
        private Assertions assertions;

        public SampleModel(String id, String name, String inputPath, String outputPath, String outputTabulatedPath, Assertions assertions) {
            this.id = id;
            this.name = name;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.outputTabulatedPath = outputTabulatedPath;
            this.assertions = assertions;
        }

        public String getId() {
            return id;
        }
        public String getName() {
            return name;
        }
        public String getInputPath() {
            return inputPath;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public String getOutputTabulatedPath() {
            return outputTabulatedPath;
        }

        public Assertions getAssertions() {
            return assertions;
        }

        public class Assertions {

            private Integer modelValidationFailures;
            private boolean schemaValidationFailure;
            private boolean runtimeError;

            public Assertions(Integer modelValidationFailures, boolean schemaValidationFailure, boolean runtimeError) {
                this.modelValidationFailures = modelValidationFailures;
                this.schemaValidationFailure = schemaValidationFailure;
                this.runtimeError = runtimeError;
            }

            public Integer getModelValidationFailures() {
                return modelValidationFailures;
            }

            public void setModelValidationFailures(Integer modelValidationFailures) {
                this.modelValidationFailures = modelValidationFailures;
            }

            public boolean isSchemaValidationFailure() {
                return schemaValidationFailure;
            }
        }
    }
}
