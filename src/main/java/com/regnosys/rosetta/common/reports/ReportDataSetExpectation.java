package com.regnosys.rosetta.common.reports;

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

import com.rosetta.model.lib.ModelReportId;

import java.util.List;

/**
 * Expectations for a report and data-set, e.g. CFTC Part 45, rates.
 */
public class ReportDataSetExpectation {

    private ModelReportId reportId;
    private String dataSetName;
    private List<ReportDataItemExpectation> dataItemExpectations;

    public ReportDataSetExpectation(ModelReportId reportId, String dataSetName, List<ReportDataItemExpectation> dataItemExpectations) {
        this.reportId = reportId;
        this.dataSetName = dataSetName;
        this.dataItemExpectations = dataItemExpectations;
    }

    private ReportDataSetExpectation() {
    }

    public ModelReportId getReportId() {
        return reportId;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public List<ReportDataItemExpectation> getDataItemExpectations() {
        return dataItemExpectations;
    }
}
