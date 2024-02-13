package com.regnosys.rosetta.common.projection;

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
