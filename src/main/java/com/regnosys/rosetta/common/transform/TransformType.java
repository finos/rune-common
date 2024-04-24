package com.regnosys.rosetta.common.transform;

import java.util.function.Function;

public enum TransformType {
    TRANSLATE("translate", "TBD"),
    PROJECTION("projection", "projections.%sProjectionTabulator"),
    REPORT("regulatory-reporting", "reports.%sReportTabulator", stripReportFunctionName());

    private final String resourcePath;
    private final String tabulatorName;
    private final Function<String,String> transformFunctionName;

    TransformType(String resourcePath, String tabulatorName) {
        this.resourcePath = resourcePath;
        this.tabulatorName = tabulatorName;
        this.transformFunctionName = Function.identity();
    }

    TransformType(String resourcePath, String tabulatorName, Function<String, String> transformFunctionName) {
        this.resourcePath = resourcePath;
        this.tabulatorName = tabulatorName;
        this.transformFunctionName = transformFunctionName;
    }

    /**
     * This is the path in either github, or local file system where the transform files are located.
     *
     * @return the path to the transform
     */
    public String getResourcePath() {
        return resourcePath;
    }

    public String getTabulatorName(String functionName) {
        return String.format(tabulatorName, transformFunctionName.apply(functionName));
    }

    private static Function<String, String> stripReportFunctionName() {
        return functionName -> functionName.replaceAll("ReportFunction$", "");
    }
}
