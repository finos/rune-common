package com.regnosys.rosetta.common.serialisation.reportdata;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rosetta.model.lib.ModelReportId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpectedResult {
    private Map<ModelReportId, List<ExpectedResultField>> expectationsPerReport;

    public ExpectedResult(){}

    @JsonCreator
    public ExpectedResult(@JsonProperty Map<ModelReportId, List<ExpectedResultField>> expectationsPerReport) {
        this.expectationsPerReport = expectationsPerReport;
    }

    public Map<ModelReportId, List<ExpectedResultField>> getExpectationsPerReport() {
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
