package com.regnosys.rosetta.common.serialisation.projectiondata;

import com.regnosys.rosetta.common.serialisation.DataSet;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.rosetta.model.lib.ModelReportId;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ProjectionDataSet extends DataSet {

    private String applicableProjection;


    public ProjectionDataSet(String dataSetName, String dataSetShortName, String inputType, String applicableProjection, List<ReportDataItem> data) {
       super(dataSetName, dataSetShortName, inputType, data);
        this.applicableProjection = applicableProjection;
    }

    public ProjectionDataSet() {
    }

    public String getApplicableProjection() {
        return applicableProjection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectionDataSet)) return false;
        if (!super.equals(o)) return false;
        ProjectionDataSet that = (ProjectionDataSet) o;
        return Objects.equals(getApplicableProjection(), that.getApplicableProjection());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getApplicableProjection());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProjectionDataSet.class.getSimpleName() + "[", "]")
                .add("dataSetName='" + getDataSetName() + "'")
                .add("inputType='" + getInputType() + "'")
                .add("expectedType='" + getExpectedType() + "'")
                .add("applicableReports=" + applicableProjection)
                .add("data=" + getData())
                .toString();
    }
}
