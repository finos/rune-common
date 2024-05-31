package com.regnosys.rosetta.common;

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

import com.regnosys.rosetta.common.reports.RegReportPaths;
import com.rosetta.model.lib.ModelReportId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RegPaths {

    public static final Path INPUT_PATH = Paths.get("input");
    public static final Path OUTPUT_PATH = Paths.get("output");
    public static final Path CONFIG_PATH = Paths.get("config");
    public static final Path LOOKUP_PATH = Paths.get("lookup");

    // Legacy folder structure
    public static final Path LEGACY_DATA_PATH = Paths.get("data");
    public static final String KEY_VALUE_FILE_NAME_SUFFIX = "-key-value.json";
    public static final String REPORT_FILE_NAME_SUFFIX = "-report.json";
    private final Path rootPath;
    private final Path input;
    private final Path output;
    private final Path config;
    private final Path lookup;

    public RegPaths(Path rootPath, Path input, Path output, Path config, Path lookup) {
        this.rootPath = rootPath;
        this.config = config;
        this.input = input;
        this.output = output;
        this.lookup = lookup;
    }

    public Path getRootRelativePath() {
        return rootPath;
    }

    public Path getConfigRelativePath() {
        return config;
    }

    public Path getInputRelativePath() {
        return input;
    }

    public Path getOutputRelativePath() {
        return output;
    }

    public Path getLookupRelativePath() {
        return lookup;
    }

    public static String directoryNameOfDataset(String datasetName) {
        return datasetName
                .replace(" ", "-")
                .replace("_", "-")
                .trim().toLowerCase();
    }

    public static String directoryName(ModelReportId id) {
        return id.joinRegulatoryReference("-")
                .replace("_", "-")
                .toLowerCase();
    }

    public static Path getOutputPath(Path outputPath, ModelReportId reportIdentifier) {
        return outputPath.resolve(directoryName(reportIdentifier));
    }

    public static Path getOutputDataSetPath(Path outputPath, ModelReportId reportIdentifier, String dataSetName) {
        return getOutputPath(outputPath, reportIdentifier).resolve(directoryNameOfDataset(dataSetName));
    }

    public static Path getInputDataSetPath(Path inputPath, String dataSetName) {
        return inputPath.resolve(directoryNameOfDataset(dataSetName));
    }


    public static Path getKeyValueExpectationFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName, Path inputPath) {
        return getOutputDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", KEY_VALUE_FILE_NAME_SUFFIX));
    }
}
