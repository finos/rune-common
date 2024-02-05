package com.regnosys.rosetta.common;

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
}
