package com.regnosys.rosetta.common.transform;

import java.util.Objects;

public class PipelineModel {

    private final String id;
    private final String name;
    private final Transform transform;
    private final String upstreamPipelineId;
    private final Serialisation outputSerialisation;

    public PipelineModel(String id, String name, Transform transform, String upstreamPipelineId, Serialisation outputSerialisation) {
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

        public Transform(TransformType type, String function, String inputType, String outputType) {
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

        public Serialisation(Format format, String configPath) {
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

        public enum  Format {
            JSON,
            XML
        }
    }
}

