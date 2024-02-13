package com.regnosys.rosetta.common.projection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ProjectionDataSetExpectation {
    private final String projectionName;
    private final String dataSetName;
    private final List<ProjectionDataItemExpectation> dataItemExpectations;

    @JsonCreator
    public ProjectionDataSetExpectation(@JsonProperty("projectionName") String projectionName,
                                        @JsonProperty("dataSetName") String dataSetName,
                                        @JsonProperty("dataItemExpectations") List<ProjectionDataItemExpectation> dataItemExpectations) {
        this.projectionName = projectionName;
        this.dataSetName = dataSetName;
        this.dataItemExpectations = dataItemExpectations;
    }

    public String getProjectionName() {
        return projectionName;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public List<ProjectionDataItemExpectation> getDataItemExpectations() {
        return dataItemExpectations;
    }
}
