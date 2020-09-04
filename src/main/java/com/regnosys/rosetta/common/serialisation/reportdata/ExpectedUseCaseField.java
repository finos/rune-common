package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.Objects;
import java.util.StringJoiner;

public class ExpectedUseCaseField {
    private String key;
    private String value;

    public ExpectedUseCaseField(){}

    public ExpectedUseCaseField(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedUseCaseField that = (ExpectedUseCaseField) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExpectedUseCaseField.class.getSimpleName() + "[", "]")
                .add("key='" + key + "'")
                .add("value='" + value + "'")
                .toString();
    }
}
