package com.regnosys.rosetta.common.testing;

import java.util.Map;
import java.util.Objects;

public class MappingCoverage {

    private final String synonymSource;
    private final Map<String, String> schema;
    private final int mappingCoverage;

    public MappingCoverage(String synonymSource, Map<String, String> schema, int mappingCoverage) {
        this.synonymSource = synonymSource;
        this.schema = schema;
        this.mappingCoverage = mappingCoverage;
    }

    public String getSynonymSource() {
        return synonymSource;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public int getMappingCoverage() {
        return mappingCoverage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingCoverage that = (MappingCoverage) o;
        return mappingCoverage == that.mappingCoverage && Objects.equals(synonymSource, that.synonymSource) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(synonymSource, schema, mappingCoverage);
    }

    @Override
    public String toString() {
        return "MappingCoverage{" +
                "synonymSource='" + synonymSource + '\'' +
                ", schema=" + schema +
                ", mappingCoverage=" + mappingCoverage +
                '}';
    }
}
