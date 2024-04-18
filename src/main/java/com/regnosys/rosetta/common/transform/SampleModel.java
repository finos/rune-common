package com.regnosys.rosetta.common.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SampleModel {

    private final String id;
    private final String name;
    private final String inputPath;
    private final String outputPath;
    private final String outputTabulatedPath;
    private final Assertions assertions;

    @JsonCreator
    public SampleModel(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("inputPath") String inputPath,
                       @JsonProperty("outputPath") String outputPath,
                       @JsonProperty("outputTabulatedPath") String outputTabulatedPath,
                       @JsonProperty("assertions") Assertions assertions) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SampleModel)) return false;
        SampleModel that = (SampleModel) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getInputPath(), that.getInputPath()) && Objects.equals(getOutputPath(), that.getOutputPath()) && Objects.equals(getOutputTabulatedPath(), that.getOutputTabulatedPath()) && Objects.equals(getAssertions(), that.getAssertions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getInputPath(), getOutputPath(), getOutputTabulatedPath(), getAssertions());
    }

    @Override
    public String toString() {
        return "SampleModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                ", outputTabulatedPath='" + outputTabulatedPath + '\'' +
                ", assertions=" + assertions +
                '}';
    }

}