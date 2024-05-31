package com.regnosys.rosetta.common.projection;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;

public class ProjectionDataItemExpectation implements Comparable<ProjectionDataItemExpectation> {
    private String inputFile;
    private String keyValueFile;
    private String outputFile;
    private int validationFailures;
    private boolean validXml;
    private boolean error;

    @JsonCreator
    public ProjectionDataItemExpectation(@JsonProperty("inputFile") String inputFile,
                                         @JsonProperty("keyValueFile") String keyValueFile,
                                         @JsonProperty("outputFile") String outputFile,
                                         @JsonProperty("validationFailures") int validationFailures,
                                         @JsonProperty("validXml") boolean validXml,
                                         @JsonProperty("error") boolean error) {
        this.inputFile = inputFile;
        this.keyValueFile = keyValueFile;
        this.outputFile = outputFile;
        this.validationFailures = validationFailures;
        this.validXml = validXml;
        this.error = error;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getKeyValueFile() {
        return keyValueFile;
    }

    public void setKeyValueFile(String keyValueFile) {
        this.keyValueFile = keyValueFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public int getValidationFailures() {
        return validationFailures;
    }

    public void setValidationFailures(int validationFailures) {
        this.validationFailures = validationFailures;
    }

    public boolean isValidXml() {
        return validXml;
    }

    public void setValidXml(boolean validXml) {
        this.validXml = validXml;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public int compareTo(ProjectionDataItemExpectation o) {
        return Comparator.comparing(ProjectionDataItemExpectation::getInputFile).compare(this, o);
    }
}
