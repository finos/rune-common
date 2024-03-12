package com.regnosys.rosetta.common.serialisation.reportdata;

import com.regnosys.rosetta.common.serialisation.DataSet;
import com.rosetta.model.lib.ModelReportId;

import java.util.Objects;

public class ReportIdentifierDataSet {

    private ModelReportId reportIdentifier;
    private DataSet dataSet;

    public ReportIdentifierDataSet(ModelReportId reportIdentifier, DataSet dataSet) {
        this.reportIdentifier = reportIdentifier;
        this.dataSet = dataSet;
    }

    public ReportIdentifierDataSet() {
    }

    public ModelReportId getReportIdentifier() {
        return reportIdentifier;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportIdentifierDataSet that = (ReportIdentifierDataSet) o;
        return Objects.equals(reportIdentifier, that.reportIdentifier) && Objects.equals(dataSet, that.dataSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportIdentifier, dataSet);
    }

    @Override
    public String toString() {
        return "ReportIdentifierDataSet{" +
                "reportIdentifier=" + reportIdentifier +
                ", dataSet=" + dataSet +
                '}';
    }
}
