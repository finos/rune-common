package com.regnosys.rosetta.common.testing;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class MappingCoverage {

    private final String ingestionEnvironment;
    private final Map<String, String> schema;
    private final double mappingCoverage;

    public MappingCoverage(
            @JsonProperty("ingestionEnvironment") String ingestionEnvironment,
            @JsonProperty("schema") Map<String, String> schema,
            @JsonProperty("mappingCoverage") double mappingCoverage) {
        this.ingestionEnvironment = ingestionEnvironment;
        this.schema = schema;
        this.mappingCoverage = mappingCoverage;
    }

    public String getIngestionEnvironment() {
        return ingestionEnvironment;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public double getMappingCoverage() {
        return mappingCoverage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingCoverage that = (MappingCoverage) o;
        return mappingCoverage == that.mappingCoverage && Objects.equals(ingestionEnvironment, that.ingestionEnvironment) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingestionEnvironment, schema, mappingCoverage);
    }

    @Override
    public String toString() {
        return "MappingCoverage{" +
                "ingestionEnvironment='" + ingestionEnvironment + '\'' +
                ", schema=" + schema +
                ", mappingCoverage=" + mappingCoverage +
                '}';
    }
}
