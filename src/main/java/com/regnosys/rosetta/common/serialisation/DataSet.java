package com.regnosys.rosetta.common.serialisation;

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

import com.regnosys.rosetta.common.serialisation.reportdata.ExpectedResult;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.rosetta.model.lib.ModelReportId;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public abstract class DataSet {
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

    public void setData(List<ReportDataItem> data) {
        this.data = data;
    }
}
