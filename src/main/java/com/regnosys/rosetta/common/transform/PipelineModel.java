package com.regnosys.rosetta.common.transform;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

import java.util.Objects;

public class PipelineModel {

    private final String id;
    private final String name;
    private final Transform transform;
    private final String upstreamPipelineId;
    private final Serialisation outputSerialisation;

    @JsonCreator
    public PipelineModel(@JsonProperty("id") String id,
                         @JsonProperty("name") String name,
                         @JsonProperty("transform") Transform transform,
                         @JsonProperty("upstreamPipelineId") String upstreamPipelineId,
                         @JsonProperty("outputSerialisation") Serialisation outputSerialisation) {
        this.id = id;
        this.name = name;
        this.transform = transform;
        this.upstreamPipelineId = upstreamPipelineId;
        this.outputSerialisation = outputSerialisation;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Transform getTransform() {
        return transform;
    }

    public String getUpstreamPipelineId() {
        return upstreamPipelineId;
    }

    public Serialisation getOutputSerialisation() {
        return outputSerialisation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PipelineModel)) return false;
        PipelineModel that = (PipelineModel) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getTransform(), that.getTransform()) && Objects.equals(getUpstreamPipelineId(), that.getUpstreamPipelineId()) && Objects.equals(getOutputSerialisation(), that.getOutputSerialisation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getTransform(), getUpstreamPipelineId(), getOutputSerialisation());
    }

    @Override
    public String toString() {
        return "PipelineModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", transform=" + transform +
                ", upstreamPipelineId='" + upstreamPipelineId + '\'' +
                ", outputSerialisation=" + outputSerialisation +
                '}';
    }

    public static class Transform {
        private final TransformType type;
        private final String function;
        private final String inputType;
        private final String outputType;

        @JsonCreator
        public Transform(@JsonProperty("type") TransformType type,
                         @JsonProperty("function") String function,
                         @JsonProperty("inputType") String inputType,
                         @JsonProperty("outputType") String outputType) {
            this.type = type;
            this.function = function;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        public TransformType getType() {
            return type;
        }

        public String getFunction() {
            return function;
        }

        public String getInputType() {
            return inputType;
        }

        public String getOutputType() {
            return outputType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Transform)) return false;
            Transform transform = (Transform) o;
            return getType() == transform.getType() && Objects.equals(getFunction(), transform.getFunction()) && Objects.equals(getInputType(), transform.getInputType()) && Objects.equals(getOutputType(), transform.getOutputType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getType(), getFunction(), getInputType(), getOutputType());
        }

        @Override
        public String toString() {
            return "Transform{" +
                    "type=" + type +
                    ", function='" + function + '\'' +
                    ", inputType='" + inputType + '\'' +
                    ", outputType='" + outputType + '\'' +
                    '}';
        }
    }

    public static class Serialisation {
        private final Format format;
        private final String configPath;


        @JsonCreator
        public Serialisation(@JsonProperty("format") Format format,
                             @JsonProperty("configPath") String configPath) {
            this.format = format;
            this.configPath = configPath;
        }

        public Format getFormat() {
            return format;
        }

        public String getConfigPath() {
            return configPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Serialisation)) return false;
            Serialisation that = (Serialisation) o;
            return getFormat() == that.getFormat() && Objects.equals(getConfigPath(), that.getConfigPath());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFormat(), getConfigPath());
        }

        @Override
        public String toString() {
            return "Serialisation{" +
                    "format=" + format +
                    ", configPath='" + configPath + '\'' +
                    '}';
        }

        public enum Format {
            JSON,
            XML
        }
    }
}

