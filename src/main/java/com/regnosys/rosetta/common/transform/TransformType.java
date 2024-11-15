package com.regnosys.rosetta.common.transform;

/*-
 * ==============
 * Rune Common
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

import java.util.function.Function;

public enum TransformType {
    PRE_TRANSLATE("translate/pre"),
    TRANSLATE("translate", null),
    TRANSLATE_1_5("ingestions", null),
    POST_TRANSLATE("translate/post"),

    PRE_INGEST("ingest/pre"),
    INGEST("ingest", null),
    POST_INGEST("ingest/post"),
    
    PRE_ENRICH("enrich/pre"),
    ENRICH("enrich", null),
    POST_ENRICH("enrich/post"),

    PRE_REPORT("regulatory-reporting/pre", "reports.%sReportTabulator", stripReportFunctionName()),
    REPORT("regulatory-reporting", "reports.%sReportTabulator", stripReportFunctionName()),
    POST_REPORT("regulatory-reporting/post", "reports.%sReportTabulator", stripReportFunctionName()),

    PRE_PROJECTION("projection/pre", "projections.%sProjectionTabulator"),
    PROJECTION("projection", "projections.%sProjectionTabulator"),
    POST_PROJECTION("projection/post", "projections.%sProjectionTabulator");

    private final String resourcePath;
    private final String tabulatorName;
    private final Function<String,String> transformFunctionName;

    TransformType(String resourcePath) {
        this.resourcePath = resourcePath;
        this.tabulatorName = "%sTabulator";
        this.transformFunctionName = Function.identity();
    }

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


    private static Function<String, String> stripReportFunctionName() {
        return functionName -> functionName.replaceAll("ReportFunction$", "");
    }
}
