package com.regnosys.rosetta.common.reports;

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
