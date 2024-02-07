package com.regnosys.rosetta.common.projection;

import com.regnosys.rosetta.common.RegPaths;
import com.regnosys.rosetta.common.reports.RegReportPaths;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RegProjectionPaths extends RegPaths {

    public static final Path PROJECTION_PATH = Paths.get("projection");
    public static final Path ISO20022_PATH = Paths.get("iso-20022");

    public RegProjectionPaths(Path rootPath, Path input, Path output, Path config, Path lookup) {
        super(rootPath, input, output, config, lookup);
    }

    public static RegProjectionPaths getProjectionPath() {
        Path projectionPath = PROJECTION_PATH;
        Path isoPath = projectionPath.resolve(ISO20022_PATH);
        return new RegProjectionPaths(isoPath,
                isoPath.resolve(INPUT_PATH),
                isoPath.resolve(OUTPUT_PATH),
                isoPath.resolve(CONFIG_PATH),
                isoPath.resolve(LOOKUP_PATH));
    }
}
