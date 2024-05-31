package com.regnosys.rosetta.common.transform;

/*-
 * ==============
 * Rosetta Common
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
