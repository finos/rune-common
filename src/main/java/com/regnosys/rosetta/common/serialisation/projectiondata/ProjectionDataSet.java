package com.regnosys.rosetta.common.serialisation.projectiondata;

import com.regnosys.rosetta.common.serialisation.DataSet;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;

import java.util.*;

public class ProjectionDataSet extends DataSet {

    private String applicableProjection;
    private SortedSet<String> applicableProjections;

    public ProjectionDataSet(String dataSetName, String dataSetShortName, String inputType, String applicableProjection, List<ReportDataItem> data, SortedSet<String> applicableProjections) {
       super(dataSetName, dataSetShortName, inputType, data);
        this.applicableProjection = applicableProjection;
        this.applicableProjections = null==applicableProjections? Collections.emptySortedSet():applicableProjections;
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
        return Objects.equals(getApplicableProjection(), that.getApplicableProjection()) && Objects.equals(getApplicableProjections(), that.getApplicableProjections());
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
                .add("applicableProjection=" + applicableProjection)
                .add("applicableProjections=" + applicableProjections)
                .add("data=" + getData())
                .toString();
    }

    public SortedSet<String> getApplicableProjections() {
        return this.applicableProjections;

    }
}
