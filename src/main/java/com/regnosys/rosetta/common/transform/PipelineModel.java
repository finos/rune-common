package com.regnosys.rosetta.common.transform;

public record PipelineModel(String id, String name, Transform transform, String upstreamPipelineId, Serialisation outputSerialisation) {
    public record Transform(TransformType type, String function, String inputType, String outputType) {}

    public record Serialisation(Format format, String configPath) {
        public enum Format {
            JSON,
            XML
        }
    }
}

