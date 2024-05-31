package com.regnosys.rosetta.common.serialisation.reportdata;

/*-
 * #%L
 * Rosetta Common
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

import com.rosetta.model.lib.ModelReportId;

import java.util.Objects;

public class ReportIdentifierDataSet {

    private ModelReportId reportIdentifier;
    private ReportDataSet dataSet;

    public ReportIdentifierDataSet(ModelReportId reportIdentifier, ReportDataSet dataSet) {
        this.reportIdentifier = reportIdentifier;
        this.dataSet = dataSet;
    }

    public ReportIdentifierDataSet() {
    }

    public ModelReportId getReportIdentifier() {
        return reportIdentifier;
    }

    public ReportDataSet getDataSet() {
        return dataSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportIdentifierDataSet that = (ReportIdentifierDataSet) o;
        return Objects.equals(reportIdentifier, that.reportIdentifier) && Objects.equals(dataSet, that.dataSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportIdentifier, dataSet);
    }

    @Override
    public String toString() {
        return "ReportIdentifierDataSet{" +
                "reportIdentifier=" + reportIdentifier +
                ", dataSet=" + dataSet +
                '}';
    }
}
