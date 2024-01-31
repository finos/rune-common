package com.regnosys.rosetta.common.serialisation.reportdata;

import com.rosetta.model.lib.ModelReportId;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ReportDataSet {
    private final static String EXPECTED_TYPE = ExpectedResult.class.getName();

    private String dataSetName;

    private String dataSetShortName;
    private String inputType;

    private String applicableProjection;
    private List<ModelReportId> applicableReports;
    private List<ReportDataItem> data;

    public ReportDataSet(String dataSetName, String dataSetShortName, String inputType, List<ModelReportId> applicableReports, List<ReportDataItem> data, String applicableProjection) {
        this.dataSetName = dataSetName;

        if(null != dataSetShortName && !dataSetShortName.isEmpty()){
            this.dataSetShortName = dataSetShortName;
        }
        else{
            this.dataSetShortName = dataSetName;
        }
        this.inputType = inputType;
        this.applicableReports = applicableReports;
        this.data = data;
        this.applicableProjection = applicableProjection;
    }

    public ReportDataSet(String dataSetName, String inputType, String applicableProjection, List<ReportDataItem> data ) {
        this.dataSetName = dataSetName;
        this.dataSetShortName = dataSetName;
        this.inputType = inputType;
        this.data = data;
        this.applicableProjection = applicableProjection;
    }

    public ReportDataSet(String dataSetName, String inputType, List<ModelReportId> applicableReports, List<ReportDataItem> data) {
        this.dataSetName = dataSetName;
        this.dataSetShortName = dataSetName;
        this.inputType = inputType;
        this.applicableReports = applicableReports;
        this.data = data;
    }

    public String getDataSetShortName() {
        return dataSetShortName;
    }

    public ReportDataSet() {
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public String getInputType() {
        return inputType;
    }

    public String getExpectedType() {
        return EXPECTED_TYPE;
    }

    public List<ModelReportId> getApplicableReports() {
        return applicableReports;
    }

    public List<ReportDataItem> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportDataSet that = (ReportDataSet) o;
        return Objects.equals(dataSetName, that.dataSetName) &&
                Objects.equals(dataSetShortName, that.dataSetShortName) &&
                Objects.equals(inputType, that.inputType) &&
                Objects.equals(applicableReports, that.applicableReports) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSetName, dataSetShortName, inputType, EXPECTED_TYPE, applicableReports, data);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportDataSet.class.getSimpleName() + "[", "]")
                .add("dataSetName='" + dataSetName + "'")
                .add("inputType='" + inputType + "'")
                .add("expectedType='" + EXPECTED_TYPE + "'")
                .add("applicableReports=" + applicableReports)
                .add("data=" + data)
                .toString();
    }
}
