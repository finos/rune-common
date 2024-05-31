package com.regnosys.rosetta.common.projection;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
