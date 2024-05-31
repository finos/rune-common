package com.regnosys.rosetta.common.serialisation.reportdata;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

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
