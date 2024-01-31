package com.regnosys.rosetta.common.serialisation;

import com.regnosys.rosetta.common.serialisation.reportdata.ExpectedResult;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class DataSet {
    private final static String EXPECTED_TYPE = ExpectedResult.class.getName();

    private String dataSetName;

    private String dataSetShortName;
    private String inputType;
    private List<ReportDataItem> data;

    public DataSet(String dataSetName, String dataSetShortName, String inputType, List<ReportDataItem> data) {
        this.dataSetName = dataSetName;

        if(null != dataSetShortName && !dataSetShortName.isEmpty()){
            this.dataSetShortName = dataSetShortName;
        }
        else{
            this.dataSetShortName = dataSetName;
        }
        this.inputType = inputType;
        this.data = data;
    }

    public DataSet(String dataSetName, String inputType, List<ReportDataItem> data) {
        this.dataSetName = dataSetName;
        this.dataSetShortName = dataSetName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSet that = (DataSet) o;
        return Objects.equals(dataSetName, that.dataSetName) &&
                Objects.equals(dataSetShortName, that.dataSetShortName) &&
                Objects.equals(inputType, that.inputType) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSetName, dataSetShortName, inputType, EXPECTED_TYPE, data);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DataSet.class.getSimpleName() + "[", "]")
                .add("dataSetName='" + dataSetName + "'")
                .add("inputType='" + inputType + "'")
                .add("expectedType='" + EXPECTED_TYPE + "'")
                .add("data=" + data)
                .toString();
    }
}
