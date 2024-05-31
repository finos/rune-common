package com.regnosys.rosetta.common.serialisation.reportdata;

/*-
 * ==============
 * Rosetta Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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
 * ==============
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.*;

@Deprecated
public class JsonReportDataLoader extends AbstractJsonDataLoader<ReportDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";

    private final URL inputPath;

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, ReportDataSet.class, false);
        this.inputPath = null;
    }

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames,
                                URL inputPath) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, ReportDataSet.class, true);
        this.inputPath = inputPath;
    }

    @Override
    public ReportDataSet loadInputFiles(ReportDataSet descriptor) {
        List<ReportDataItem> loadedData = getDataItem(descriptor, inputPath);
        return new ReportDataSet(descriptor.getDataSetName(), descriptor.getInputType(), descriptor.getApplicableReports(), loadedData);
    }
}
