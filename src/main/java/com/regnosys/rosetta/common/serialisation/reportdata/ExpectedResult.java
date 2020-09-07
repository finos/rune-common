package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ExpectedResult {
    private List<ExpectedResultField> fields;

    public ExpectedResult(){}

    public ExpectedResult(List<ExpectedResultField> fields) {
        this.fields = fields;
    }

    public List<ExpectedResultField> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedResult that = (ExpectedResult) o;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFields());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExpectedResult.class.getSimpleName() + "[", "]")
                .add("fields=" + fields)
                .toString();
    }
}
