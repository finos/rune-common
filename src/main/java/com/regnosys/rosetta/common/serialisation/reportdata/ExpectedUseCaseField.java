package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.Objects;
import java.util.StringJoiner;

public class ExpectedUseCaseField {
    private String name;
    private String value;

    public ExpectedUseCaseField(){}

    public ExpectedUseCaseField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedUseCaseField that = (ExpectedUseCaseField) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExpectedUseCaseField.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("value='" + value + "'")
                .toString();
    }
}
