package com.regnosys.rosetta.common.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class PipelineInfo {

    private final String upstreamPipelineId;

    PipelineModel.Serialisation serialisation;

    @JsonCreator
    public PipelineInfo(@JsonProperty("upstreamPipelineId") String upstreamPipelineId,
                        @JsonProperty("serialisation") PipelineModel.Serialisation serialisation) {


        this.upstreamPipelineId = upstreamPipelineId;
        this.serialisation = serialisation;
    }

    public String getUpstreamPipelineId() {
        return upstreamPipelineId;
    }

    public PipelineModel.Serialisation getSerialisation() {
        return serialisation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PipelineInfo)) return false;
        PipelineInfo that = (PipelineInfo) o;
        return Objects.equals(getUpstreamPipelineId(), that.getUpstreamPipelineId()) && Objects.equals(getSerialisation(), that.getSerialisation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUpstreamPipelineId(), getSerialisation());
    }
}
