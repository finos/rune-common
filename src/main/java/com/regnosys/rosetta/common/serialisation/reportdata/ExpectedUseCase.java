package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ExpectedUseCase {
    private List<ExpectedUseCaseField> fields;

    public ExpectedUseCase(){}

    public ExpectedUseCase(List<ExpectedUseCaseField> fields) {
        this.fields = fields;
    }

    public List<ExpectedUseCaseField> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedUseCase that = (ExpectedUseCase) o;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFields());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExpectedUseCase.class.getSimpleName() + "[", "]")
                .add("fields=" + fields)
                .toString();
    }
}
