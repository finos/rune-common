package com.regnosys.rosetta.common.transform;

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

    public class Transform {
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

        public enum  Format {
            JSON,
            XML
        }
    }
}

