package com.regnosys.rosetta.common.transform;

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

}

