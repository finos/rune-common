package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ReportDataSet {
    private String dataSetName;
    private String inputType;
    private String applicableReports;
    private List<ReportDataItem> data;

    public ReportDataSet(String dataSetName, String inputType, String applicableReports, List<ReportDataItem> data) {
        this.dataSetName = dataSetName;
        this.inputType = inputType;
        this.applicableReports = applicableReports;
        this.data = data;
    }

    public ReportDataSet() {
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public String getInputType() {
        return inputType;
    }

    public String getApplicableReports() {
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
                Objects.equals(inputType, that.inputType) &&
                Objects.equals(applicableReports, that.applicableReports) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSetName, inputType, applicableReports, data);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportDataSet.class.getSimpleName() + "[", "]")
                .add("dataSetName='" + dataSetName + "'")
                .add("inputType='" + inputType + "'")
                .add("applicableReports='" + applicableReports + "'")
                .add("data=" + data)
                .toString();
    }
}
