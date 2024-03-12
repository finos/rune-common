package com.regnosys.rosetta.common.serialisation;

import com.google.common.collect.Lists;
import com.regnosys.rosetta.common.serialisation.reportdata.ExpectedResult;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.rosetta.model.lib.ModelReportId;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class DataSet {
    private final static String EXPECTED_TYPE = ExpectedResult.class.getName();

    private String dataSetName;
    private String dataSetShortName;
    private String inputType;
    private List<ReportDataItem> data;
    private List<ModelReportId> applicableReports;
    private List<String> applicableProjections;

    public DataSet(String dataSetName, String inputType, List<ReportDataItem> data, List<ModelReportId> applicableReports, List<String> applicableProjections) {
        this(dataSetName, dataSetName, inputType, data, applicableReports, applicableProjections);
    }

    public DataSet(String dataSetName, String dataSetShortName, String inputType, List<ReportDataItem> data, List<ModelReportId> applicableReports, List<String> applicableProjections) {
        this.dataSetName = dataSetName;
        this.applicableReports = applicableReports == null ? Lists.newArrayList() : applicableReports;
        this.applicableProjections = applicableProjections == null ? Lists.newArrayList() : applicableProjections;
        if(null != dataSetShortName && !dataSetShortName.isEmpty()){
            this.dataSetShortName = dataSetShortName;
        }
        else{
            this.dataSetShortName = dataSetName;
        }
        this.inputType = inputType;
        this.data = data;
    }
    public String getDataSetShortName() {
        return dataSetShortName;
    }

    public DataSet() {
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

    public List<ReportDataItem> getData() {
        return data;
    }

    public List<ModelReportId> getApplicableReports() {
        return applicableReports;
    }

    public List<String> getApplicableProjections() {
        return applicableProjections;
    }

    public void setData(List<ReportDataItem> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DataSet{" +
                "dataSetName='" + dataSetName + '\'' +
                ", dataSetShortName='" + dataSetShortName + '\'' +
                ", inputType='" + inputType + '\'' +
                ", data=" + data +
                ", applicableReports=" + applicableReports +
                ", applicableProjections=" + applicableProjections +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSet dataSet = (DataSet) o;
        return Objects.equals(dataSetName, dataSet.dataSetName) && Objects.equals(dataSetShortName, dataSet.dataSetShortName) && Objects.equals(inputType, dataSet.inputType) && Objects.equals(data, dataSet.data) && Objects.equals(applicableReports, dataSet.applicableReports) && Objects.equals(applicableProjections, dataSet.applicableProjections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSetName, dataSetShortName, inputType, data, applicableReports, applicableProjections);
    }
}
