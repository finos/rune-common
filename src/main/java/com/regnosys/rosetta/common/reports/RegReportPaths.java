package com.regnosys.rosetta.common.reports;

import com.regnosys.rosetta.common.util.UrlUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class RegReportPaths {

    public static final Path REGULATORY_REPORTING_PATH = Path.of("regulatory-reporting");
    public static final Path INPUT_PATH = Path.of("input");
    public static final Path OUTPUT_PATH = Path.of("output");
    public static final Path CONFIG_PATH = Path.of("config");

    // Legacy folder structure
    public static final Path LEGACY_DATA_PATH = Path.of("data");

    public static final String REPORT_EXPECTATIONS_FILE_NAME = "report-expectations.json";
    public static final String KEY_VALUE_FILE_NAME_SUFFIX = "-key-value.json";
    public static final String REPORT_FILE_NAME_SUFFIX = "-report.json";

    public static RegReportPaths get(URL resourcesPath) {
        return Files.exists(UrlUtils.toPath(resourcesPath).resolve(REGULATORY_REPORTING_PATH).resolve(INPUT_PATH)) ?
                RegReportPaths.getDefault() : RegReportPaths.getLegacy();
    }

    public static RegReportPaths getDefault() {
        Path rootPath = REGULATORY_REPORTING_PATH;
        return new RegReportPaths(rootPath,
                rootPath.resolve(INPUT_PATH),
                rootPath.resolve(OUTPUT_PATH),
                rootPath.resolve(CONFIG_PATH));
    }

    public static RegReportPaths getLegacy() {
        Path rootPath = REGULATORY_REPORTING_PATH.resolve(LEGACY_DATA_PATH);
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

    public static Path getReportPath(Path outputPath, String reportIdentifierName) {
        return outputPath.resolve(directoryName(reportIdentifierName));
    }

    public static Path getReportDataSetPath(Path outputPath, RegReportIdentifier reportIdentifier, String dataSetName) {
        return getReportPath(outputPath, reportIdentifier.getName()).resolve(directoryName(dataSetName));
    }

    public static Path getReportExpectationsFilePath(Path outputPath, RegReportIdentifier reportIdentifier, String dataSetName) {
        return getReportDataSetPath(outputPath, reportIdentifier, dataSetName).resolve(REPORT_EXPECTATIONS_FILE_NAME);
    }

    public static Path getKeyValueExpectationFilePath(Path outputPath, RegReportIdentifier reportIdentifier, String dataSetName, Path inputPath) {
        return getReportDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", KEY_VALUE_FILE_NAME_SUFFIX));
    }

    public static Path getReportExpectationFilePath(Path outputPath, RegReportIdentifier reportIdentifier, String dataSetName, Path inputPath) {
        return getReportDataSetPath(outputPath, reportIdentifier, dataSetName)
                .resolve(inputPath.getFileName().toString().replace(".json", REPORT_FILE_NAME_SUFFIX));
    }

    private static String directoryName(String name) {
        return name
                .replace(" ", "-")
                .replace("_", "-")
                .trim().toLowerCase();
    }
}
