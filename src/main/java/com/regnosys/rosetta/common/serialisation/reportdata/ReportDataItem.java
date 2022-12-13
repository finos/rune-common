package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.*;

public class ReportDataItem {

    private String name;
    private Object input;
    private Object expected;

    public ReportDataItem() {
    }

    public ReportDataItem(String name, Object input, Object expected) {
        this.name = name;
        this.input = input;
        this.expected = expected;
    }

    public String getName() {
        return name;
    }

    public Object getInput() {
        return input;
    }

    @Deprecated
    public Object getExpected() {
        return expected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportDataItem that = (ReportDataItem) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(input, that.input) &&
                Objects.equals(expected, that.expected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, input, expected);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportDataItem.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("input=" + input)
                .add("expected=" + (expected == null ? "" : expected.toString()))
                .toString();
    }
}
