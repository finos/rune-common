package com.regnosys.rosetta.common.serialisation.projectiondata;

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

import com.regnosys.rosetta.common.serialisation.DataSet;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.rosetta.model.lib.ModelReportId;

import java.util.*;

public class ProjectionDataSet extends DataSet {

    private List<ModelReportId> applicableReports;
    private List<String> applicableProjections;

    public ProjectionDataSet(String dataSetName, String dataSetShortName, String inputType, List<String> applicableProjections, List<ModelReportId> applicableReports, List<ReportDataItem> data) {
       super(dataSetName, dataSetShortName, inputType, data);
        this.applicableReports = applicableReports == null ? Collections.emptyList() : applicableReports;
        this.applicableProjections = applicableProjections == null ? Collections.emptyList() : applicableProjections;
    }

    public ProjectionDataSet() {
    }

    public List<ModelReportId> getApplicableReports() {
        return applicableReports;
    }

    public List<String> getApplicableProjections() {
        return applicableProjections;
    }
    @Override
    public String toString() {
        return new StringJoiner(", ", ProjectionDataSet.class.getSimpleName() + "[", "]")
                .add("dataSetName='" + getDataSetName() + "'")
                .add("inputType='" + getInputType() + "'")
                .add("expectedType='" + getExpectedType() + "'")
                .add("applicableReports=" + applicableReports)
                .add("applicableProjections=" + applicableProjections)
                .add("data=" + getData())
                .toString();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ProjectionDataSet that = (ProjectionDataSet) o;
        return Objects.equals(applicableProjections, that.applicableProjections) && Objects.equals(applicableReports, that.applicableReports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), applicableProjections, applicableReports);
    }
}
