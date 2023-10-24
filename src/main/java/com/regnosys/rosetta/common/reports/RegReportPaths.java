package com.regnosys.rosetta.common.reports;

import com.rosetta.model.lib.ModelReportId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RegReportPaths {

    public static final Path REGULATORY_REPORTING_PATH = Paths.get("regulatory-reporting");
    public static final Path INPUT_PATH = Paths.get("input");
    public static final Path OUTPUT_PATH = Paths.get("output");
    public static final Path CONFIG_PATH = Paths.get("config");
    public static final Path LOOKUP_PATH = Paths.get("lookup");

    // Legacy folder structure
    public static final Path LEGACY_DATA_PATH = Paths.get("data");

    public static final String REPORT_EXPECTATIONS_FILE_NAME = "report-expectations.json";
    public static final String KEY_VALUE_FILE_NAME_SUFFIX = "-key-value.json";
    public static final String REPORT_FILE_NAME_SUFFIX = "-report.json";

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

    private final Path rootPath;
    private final Path input;
    private final Path output;
    private final Path config;
    private final Path lookup;

    public RegReportPaths(Path rootPath, Path input, Path output, Path config, Path lookup) {
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

    public static Path getReportPath(Path outputPath, ModelReportId reportIdentifier) {
        return outputPath.resolve(directoryName(reportIdentifier));
    }
    @Deprecated
    public static Path getLegacyReportPath(Path outputPath, ModelReportId reportIdentifier) {
        return outputPath.resolve(legacyDirectoryName(reportIdentifier));
    }

    public static Path getReportDataSetPath(Path outputPath, ModelReportId reportIdentifier, String dataSetName) {
        return getReportPath(outputPath, reportIdentifier).resolve(directoryNameOfDataset(dataSetName));
    }
    @Deprecated
    public static Path getLegacyReportDataSetPath(Path outputPath, ModelReportId reportIdentifier, String dataSetName) {
        return getLegacyReportPath(outputPath, reportIdentifier).resolve(directoryNameOfDataset(dataSetName));
    }

    public static Path getReportExpectationsFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName) {
        return getReportDataSetPath(outputPath, reportIdentifier, dataSetName).resolve(REPORT_EXPECTATIONS_FILE_NAME);
    }

    public static Path getKeyValueExpectationFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName, Path inputPath) {
        return getReportDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", KEY_VALUE_FILE_NAME_SUFFIX));
    }
    @Deprecated
    public static Path getLegacyKeyValueExpectationFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName, Path inputPath) {
        return getLegacyReportDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", KEY_VALUE_FILE_NAME_SUFFIX));
    }

    public static Path getReportExpectationFilePath(Path outputPath, ModelReportId reportIdentifier, String dataSetName, Path inputPath) {
        return getReportDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", REPORT_FILE_NAME_SUFFIX));
    }

    public static String directoryName(ModelReportId id) {
        return id.joinRegulatoryReference("-")
                .replace("_", "-")
                .toLowerCase();
    }
    @Deprecated
    public static String legacyDirectoryName(ModelReportId id) {
        return id.joinRegulatoryReference("", "-")
                .replace("_", "-")
                .toLowerCase();
    }

    public static String directoryNameOfDataset(String datasetName) {
        return datasetName
                .replace(" ", "-")
                .replace("_", "-")
                .trim().toLowerCase();
    }
}
