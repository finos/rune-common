package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.xtext.xbase.lib.Exceptions;

import java.util.*;

public class ReportDataItem {

    private String name;
    private Object input;
    private Object expected;

    @JsonIgnore
    private Exception error;


    public ReportDataItem() {
    }

    public ReportDataItem(String name, Object input, Object expected) {
        this(name, input, expected, null);
    }
    public ReportDataItem(String name, Object input, Object expected, Exception error) {
        this.name = name;
        this.input = input;
        this.expected = expected;
        this.error = error;
    }

    public String getName() {
        return name;
    }

    public Object getInput() {
        if(error != null){
            Exceptions.sneakyThrow(error);
        }
        return input;
    }

    public Object getExpected() {
        return expected;
    }

    @JsonIgnore
    public Exception getError() {
        return error;
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
