package com.regnosys.rosetta.common.reports;

import com.regnosys.rosetta.common.util.UrlUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class RegReportPaths {

    private static final Path REGULATORY_REPORTING_PATH = Path.of("regulatory-reporting");
    private static final Path INPUT_PATH = Path.of("input");
    private static final Path OUTPUT_PATH = Path.of("output");
    private static final Path CONFIG_PATH = Path.of("config");

    // Legacy folder structure
    private static final Path LEGACY_DATA_PATH = Path.of("data");

    public static final String REPORT_EXPECTATIONS_FILE_NAME = "report-expectations.json";
    private static final String KEY_VALUE_FILE_NAME_SUFFIX = "-key-value.json";
    private static final String REPORT_FILE_NAME_SUFFIX = "-report.json";

    public static RegReportPaths get(URL resourcesPath) {
        return Files.exists(UrlUtils.toPath(resourcesPath).resolve(REGULATORY_REPORTING_PATH).resolve(INPUT_PATH)) ?
                RegReportPaths.getDefault() : RegReportPaths.getLegacy();
    }

    public static RegReportPaths getDefault() {
        return getDefault(REGULATORY_REPORTING_PATH);
    }

    public static RegReportPaths getDefault(Path rootPath) {
        return new RegReportPaths(rootPath, rootPath.resolve(INPUT_PATH), rootPath.resolve(OUTPUT_PATH), rootPath.resolve(CONFIG_PATH));
    }

    public static RegReportPaths getLegacy() {
        return getLegacy(REGULATORY_REPORTING_PATH.resolve(LEGACY_DATA_PATH));
    }

    public static RegReportPaths getLegacy(Path rootPath) {
        return new RegReportPaths(rootPath, rootPath, rootPath, rootPath);
    }

    private final Path rootPath;
    private final Path input;
    private final Path output;
    private final Path config;

    public RegReportPaths(Path rootPath, Path input, Path output, Path config) {
        this.rootPath = rootPath;
        this.config = config;
        this.input = input;
        this.output = output;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Path getConfigPath() {
        return config;
    }

    public Path getDescriptorPath(String fileName) {
        return config.resolve(fileName);
    }

    public Path getInputPath() {
        return input;
    }

    public Path getOutputPath() {
        return output;
    }

    public Path getReportPath(String reportIdentifierName) {
        return output.resolve(directoryName(reportIdentifierName));
    }

    public Path getReportDataSetPath(RegReportIdentifier reportIdentifier, String dataSetName) {
        return getReportPath(reportIdentifier.getName()).resolve(directoryName(dataSetName));
    }

    public Path getReportExpectationsFilePath(RegReportIdentifier reportIdentifier, String dataSetName) {
        return getReportDataSetPath(reportIdentifier, dataSetName).resolve(REPORT_EXPECTATIONS_FILE_NAME);
    }

    public Path getKeyValueExpectationFilePath(RegReportIdentifier reportIdentifier, String dataSetName, Path inputPath) {
        return getReportDataSetPath(reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", KEY_VALUE_FILE_NAME_SUFFIX));
    }

    public Path getReportExpectationFilePath(RegReportIdentifier reportIdentifier, String dataSetName, Path inputPath) {
        return getReportDataSetPath(reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", REPORT_FILE_NAME_SUFFIX));
    }

    private String directoryName(String name) {
        return name
                .replace(" ", "-")
                .replace("_", "-")
                .trim().toLowerCase();
    }
}
