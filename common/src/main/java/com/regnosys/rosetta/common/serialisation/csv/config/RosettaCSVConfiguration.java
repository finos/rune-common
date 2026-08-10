package com.regnosys.rosetta.common.serialisation.csv.config;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Global settings governing how CSV is read and written: dialect, header style, the delimiter
 * used within a single delimited-list column, which tokens deserialise to an absent value, and
 * whether a file has a header row at all.
 *
 * <p>This is deliberately global-only. It carries no per-type or per-attribute map: column↔attribute
 * binding lives in the model as labels, column order comes from attribute declaration order, and
 * scalar type and conversion come from the attribute type. Adding a keyed layer would reintroduce
 * the drift failure mode a per-type map has elsewhere — an entry naming a type or attribute that no
 * longer exists, silently ignored at lookup rather than reported — for no benefit here, since
 * everything this class configures is already global. See STORY-1932 §4.</p>
 *
 * <p>An empty configuration ({@code {}}) is behaviourally indistinguishable from RFC 4180 with no
 * configuration at all supplied — every default below is today's hard-coded behaviour, not a new
 * choice. That is what makes introducing this class a no-behaviour-change commit.</p>
 */
public class RosettaCSVConfiguration {
    public static final String DEFAULT_LIST_DELIMITER = ";";
    public static final boolean DEFAULT_HAS_HEADER = true;
    public static final HeaderStyle DEFAULT_HEADER_STYLE = HeaderStyle.ATTRIBUTE_NAME;

    /**
     * The RFC 4180 / today's-hard-coded-behaviour configuration.
     */
    public static final RosettaCSVConfiguration EMPTY = new RosettaCSVConfiguration();

    private final CsvDialect dialect;
    private final HeaderStyle headerStyle;
    private final String listDelimiter;
    private final List<String> nullTokens;
    private final boolean hasHeader;

    public RosettaCSVConfiguration() {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * @param hasHeader whether the file being read (or, on write, the file being produced) has a
     *                  header row. This describes the file, not a preference: a CSV file cannot be
     *                  sniffed for a header — a header row of codes looks like a data row — so it
     *                  has to be declared, and this is where. See STORY-1932 §3.4(A) for why this
     *                  setting exists at all, reversing an earlier design decision that it should not.
     */
    @JsonCreator
    public RosettaCSVConfiguration(
            @JsonProperty("dialect") Optional<CsvDialect> dialect,
            @JsonProperty("headerStyle") Optional<HeaderStyle> headerStyle,
            @JsonProperty("listDelimiter") Optional<String> listDelimiter,
            @JsonProperty("nullTokens") Optional<List<String>> nullTokens,
            @JsonProperty("hasHeader") Optional<Boolean> hasHeader) {
        this.dialect = dialect.orElse(CsvDialect.RFC_4180);
        this.headerStyle = headerStyle.orElse(DEFAULT_HEADER_STYLE);
        this.listDelimiter = listDelimiter.orElse(DEFAULT_LIST_DELIMITER);
        this.nullTokens = nullTokens.orElse(Collections.singletonList(""));
        this.hasHeader = hasHeader.orElse(DEFAULT_HAS_HEADER);
        if (!this.hasHeader && this.headerStyle == HeaderStyle.LABEL) {
            throw new IllegalArgumentException(
                    "Invalid RosettaCSVConfiguration: hasHeader=false is incompatible with headerStyle=LABEL. "
                            + "A label is header text; a file declared to have no header row has nowhere to put one.");
        }
    }

    /**
     * Loads a configuration from JSON. An unknown property is tolerated, not rejected — mirroring
     * {@code RosettaXMLConfiguration.load}, so a configuration document written against a newer
     * version of this class still loads against an older one.
     */
    public static RosettaCSVConfiguration load(InputStream input) throws IOException {
        ObjectMapper csvConfigurationMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addModule(new Jdk8Module()) // because RosettaCSVConfiguration contains `Optional` types.
                .serializationInclusion(JsonInclude.Include.NON_ABSENT) // because we want to interpret an absent value as `Optional.empty()`.
                .build();
        return csvConfigurationMapper.readValue(input, RosettaCSVConfiguration.class);
    }

    public CsvDialect getDialect() {
        return dialect;
    }

    public HeaderStyle getHeaderStyle() {
        return headerStyle;
    }

    public String getListDelimiter() {
        return listDelimiter;
    }

    public List<String> getNullTokens() {
        return nullTokens;
    }

    public boolean isHasHeader() {
        return hasHeader;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dialect, headerStyle, listDelimiter, nullTokens, hasHeader);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RosettaCSVConfiguration other = (RosettaCSVConfiguration) obj;
        return hasHeader == other.hasHeader
                && Objects.equals(dialect, other.dialect)
                && headerStyle == other.headerStyle
                && Objects.equals(listDelimiter, other.listDelimiter)
                && Objects.equals(nullTokens, other.nullTokens);
    }

    @Override
    public String toString() {
        return "RosettaCSVConfiguration{dialect=" + dialect + ", headerStyle=" + headerStyle
                + ", listDelimiter=" + listDelimiter + ", nullTokens=" + nullTokens + ", hasHeader=" + hasHeader + "}";
    }
}
