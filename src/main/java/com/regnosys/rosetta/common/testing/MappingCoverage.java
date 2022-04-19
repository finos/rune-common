package com.regnosys.rosetta.common.testing;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class MappingCoverage implements Comparable<MappingCoverage> {

    public static final String ENV = "env";
    public static final String DOCUMENT_NAME = "document-name";
    public static final String VERSION = "version";

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

    @Override
    public int compareTo(MappingCoverage other) {
        return Comparator.comparing(MappingCoverage::getIngestionEnvironment)
                .thenComparing(x -> x.getSchema().get(ENV))
                .thenComparing(x -> x.getSchema().get(DOCUMENT_NAME))
                .thenComparing(x -> x.getSchema().get(VERSION))
                .thenComparing(MappingCoverage::getMappingCoverage)
                .compare(this, other);
    }
}
