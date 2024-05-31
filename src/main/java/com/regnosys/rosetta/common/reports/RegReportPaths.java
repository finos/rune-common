package com.regnosys.rosetta.common.reports;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

import com.regnosys.rosetta.common.RegPaths;
import com.rosetta.model.lib.ModelReportId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RegReportPaths extends RegPaths {

    public static final Path REGULATORY_REPORTING_PATH = Paths.get("regulatory-reporting");
    public static final String REPORT_EXPECTATIONS_FILE_NAME = "report-expectations.json";


    public static RegReportPaths get(Path resourcesPath) {
        return Files.exists(resourcesPath.resolve(REGULATORY_REPORTING_PATH).resolve(INPUT_PATH)) ?
                RegReportPaths.getDefault() : RegReportPaths.getLegacy();
    }

    public static RegReportPaths getDefault() {
        Path rootPath = REGULATORY_REPORTING_PATH;
        return new RegReportPaths(rootPath,
                rootPath.resolve(INPUT_PATH),
                rootPath.resolve(OUTPUT_PATH),
                rootPath.resolve(CONFIG_PATH),
                rootPath.resolve(LOOKUP_PATH));
    }

    public static RegReportPaths getLegacy() {
        Path dataPath = REGULATORY_REPORTING_PATH.resolve(LEGACY_DATA_PATH);
        Path lookup = REGULATORY_REPORTING_PATH.resolve(LOOKUP_PATH);
        return new RegReportPaths(dataPath, dataPath, dataPath, dataPath, lookup);
    }

    public RegReportPaths(Path rootPath, Path input, Path output, Path config, Path lookup) {
        super(rootPath, input, output, config, lookup);
    }

    public static Path getReportExpectationsFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName) {
        return getOutputDataSetPath(outputPath, reportIdentifier, dataSetName).resolve(REPORT_EXPECTATIONS_FILE_NAME);
    }

    @Deprecated
    public static Path getLegacyReportPath(Path outputPath, ModelReportId reportIdentifier) {
        return outputPath.resolve(legacyDirectoryName(reportIdentifier));
    }

    @Deprecated
    public static Path getLegacyKeyValueExpectationFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName, Path inputPath) {
        return getLegacyReportDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", KEY_VALUE_FILE_NAME_SUFFIX));
    }

    public static Path getReportExpectationFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName, Path inputPath) {
        return getOutputDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", REPORT_FILE_NAME_SUFFIX));
    }


    @Deprecated
    public static String legacyDirectoryName(ModelReportId id) {
        return id.joinRegulatoryReference("", "-")
                .replace("_", "-")
                .toLowerCase();
    }
    @Deprecated
    public static Path getLegacyReportDataSetPath(Path outputPath, ModelReportId reportIdentifier, String dataSetName) {
        return getLegacyReportPath(outputPath, reportIdentifier).resolve(directoryNameOfDataset(dataSetName));
    }

}
