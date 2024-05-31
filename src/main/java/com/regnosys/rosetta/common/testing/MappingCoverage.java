package com.regnosys.rosetta.common.testing;

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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class MappingCoverage implements Comparable<MappingCoverage> {

    public static final String ENV = "env";
    public static final String DOCUMENT_NAME = "document-name";
    public static final String VERSION = "version";

    private final String ingestionEnvironment;
    private final Map<String, String> schema;
    private final double mappingCoverage;

    public MappingCoverage(
            @JsonProperty("ingestionEnvironment") String ingestionEnvironment,
            @JsonProperty("schema") Map<String, String> schema,
            @JsonProperty("mappingCoverage") double mappingCoverage) {
        this.ingestionEnvironment = ingestionEnvironment;
        this.schema = schema;
        this.mappingCoverage = mappingCoverage;
    }

    public String getIngestionEnvironment() {
        return ingestionEnvironment;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public double getMappingCoverage() {
        return mappingCoverage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingCoverage that = (MappingCoverage) o;
        return mappingCoverage == that.mappingCoverage && Objects.equals(ingestionEnvironment, that.ingestionEnvironment) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingestionEnvironment, schema, mappingCoverage);
    }

    @Override
    public String toString() {
        return "MappingCoverage{" +
                "ingestionEnvironment='" + ingestionEnvironment + '\'' +
                ", schema=" + schema +
                ", mappingCoverage=" + mappingCoverage +
                '}';
    }

    @Override
    public int compareTo(MappingCoverage other) {
        return Comparator.comparing(MappingCoverage::getIngestionEnvironment)
                .thenComparing(x -> x.getSchema().get(ENV))
                .thenComparing(x -> x.getSchema().get(DOCUMENT_NAME))
                .thenComparing(x -> x.getSchema().get(VERSION))
                .thenComparing(MappingCoverage::getMappingCoverage)
                .compare(this, other);
    }
}
