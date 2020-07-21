package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.Objects;
import java.util.StringJoiner;

public class ReportDataItem {

    private String name;
    private Object input;

    public ReportDataItem() {
    }

    public ReportDataItem(String name, Object input) {
        this.name = name;
        this.input = input;
    }

    public String getName() {
        return name;
    }

    public Object getInput() {
        return input;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportDataItem that = (ReportDataItem) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(input, that.input);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, input);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportDataItem.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("input=" + input)
                .toString();
    }
}
