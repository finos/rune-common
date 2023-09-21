package com.regnosys.rosetta.common.serialisation.json.reportdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpectedResult {
    private Map<String, List<ExpectedResultField>> expectationsPerReport;

    public ExpectedResult(){}

    @JsonCreator
    public ExpectedResult(@JsonProperty Map<String, List<ExpectedResultField>> expectationsPerReport) {
        this.expectationsPerReport = expectationsPerReport;
    }

    public Map<String, List<ExpectedResultField>> getExpectationsPerReport() {
        return expectationsPerReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedResult that = (ExpectedResult) o;
        return Objects.equals(expectationsPerReport, that.expectationsPerReport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectationsPerReport);
    }

    @Override
    public String toString() {
        return "ExpectedResult{" +
                "expectationsPerReport=" + expectationsPerReport +
                '}';
    }
}
