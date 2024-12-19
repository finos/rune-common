package com.regnosys.rosetta.common.transform;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class TestPackModel {

    private final String id;
    private final String pipelineId;
    private final String name;
    private final List<SampleModel> samples;

    @JsonCreator
    public TestPackModel(@JsonProperty("id") String id,
                         @JsonProperty("pipelineId") String pipelineId,
                         @JsonProperty("name") String name,
                         @JsonProperty("samples") List<SampleModel> samples) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestPackModel)) return false;
        TestPackModel that = (TestPackModel) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getPipelineId(), that.getPipelineId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getSamples(), that.getSamples());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getPipelineId(), getName(), getSamples());
    }

    @Override
    public String toString() {
        return "TestPackModel{" +
                "id='" + id + '\'' +
                ", pipelineId='" + pipelineId + '\'' +
                ", name='" + name + '\'' +
                ", samples=" + samples +
                '}';
    }

    public static class SampleModel {

        private final String id;
        private final String name;
        private final String inputPath;
        private final String outputPath;
        private final Assertions assertions;

        @JsonCreator
        public SampleModel(@JsonProperty("id") String id,
                           @JsonProperty("name") String name,
                           @JsonProperty("inputPath") String inputPath,
                           @JsonProperty("outputPath") String outputPath,
                           @JsonProperty("assertions") Assertions assertions) {
            this.id = id;
            this.name = name;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
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

        public Assertions getAssertions() {
            return assertions;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof SampleModel)) return false;
            SampleModel that = (SampleModel) object;
            return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getInputPath(), that.getInputPath()) && Objects.equals(getOutputPath(), that.getOutputPath()) && Objects.equals(getAssertions(), that.getAssertions());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getName(), getInputPath(), getOutputPath(), getAssertions());
        }

        @Override
        public String toString() {
            return "SampleModel{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", inputPath='" + inputPath + '\'' +
                    ", outputPath='" + outputPath + '\'' +
                    ", assertions=" + assertions +
                    '}';
        }

        public static class Assertions {

            private final Integer modelValidationFailures;
            private final Boolean schemaValidationFailure;
            private final Boolean runtimeError;

            @JsonCreator
            public Assertions(@JsonProperty("modelValidationFailures") Integer modelValidationFailures,
                              @JsonProperty("schemaValidationFailure") Boolean schemaValidationFailure,
                              @JsonProperty("runtimeError") Boolean runtimeError) {
                this.modelValidationFailures = modelValidationFailures;
                this.schemaValidationFailure = schemaValidationFailure;
                this.runtimeError = runtimeError;
            }

            public Integer getModelValidationFailures() {
                return modelValidationFailures;
            }

            public Boolean isSchemaValidationFailure() {
                return schemaValidationFailure;
            }

            public Boolean isRuntimeError() {
                return runtimeError;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Assertions)) return false;
                Assertions that = (Assertions) o;
                return Objects.equals(getModelValidationFailures(), that.getModelValidationFailures()) && Objects.equals(schemaValidationFailure, that.schemaValidationFailure) && Objects.equals(runtimeError, that.runtimeError);
            }

            @Override
            public int hashCode() {
                return Objects.hash(getModelValidationFailures(), schemaValidationFailure, runtimeError);
            }

            @Override
            public String toString() {
                return "Assertions{" +
                        "modelValidationFailures=" + modelValidationFailures +
                        ", schemaValidationFailure=" + schemaValidationFailure +
                        ", runtimeError=" + runtimeError +
                        '}';
            }
        }
    }
}
