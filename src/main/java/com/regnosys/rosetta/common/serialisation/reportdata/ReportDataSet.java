package com.regnosys.rosetta.common.serialisation.reportdata;

import com.regnosys.rosetta.common.serialisation.DataSet;
import com.rosetta.model.lib.ModelReportId;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ReportDataSet extends DataSet {

    private List<ModelReportId> applicableReports;

    public ReportDataSet(String dataSetName, String inputType, List<ModelReportId> applicableReports, List<ReportDataItem> data) {
        super(dataSetName, dataSetName, inputType, data);
        this.applicableReports = applicableReports;
    }

    public ReportDataSet() {
    }

    public List<ModelReportId> getApplicableReports() {
        return applicableReports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportDataSet)) return false;
        if (!super.equals(o)) return false;
        ReportDataSet that = (ReportDataSet) o;
        return Objects.equals(getApplicableReports(), that.getApplicableReports());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getApplicableReports());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportDataSet.class.getSimpleName() + "[", "]")
                .add("dataSetName='" + getDataSetName() + "'")
                .add("inputType='" + getInputType() + "'")
                .add("expectedType='" + getExpectedType() + "'")
                .add("applicableReports=" + applicableReports)
                .add("data=" + getData())
                .toString();
    }
}
