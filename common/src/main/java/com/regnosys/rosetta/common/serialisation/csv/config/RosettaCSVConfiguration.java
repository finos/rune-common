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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Global settings governing how CSV is read and written: dialect, header style, the delimiter
 * used within a single delimited-list column, which token deserialises to an absent value, and
 * whether a file has a header row at all — though {@code hasHeader=false} is currently refused,
 * since header-less CSV is not implemented.
 *
 * <p>This is deliberately global-only. It carries no per-type or per-attribute map: column↔attribute
 * binding lives in the model as labels, column order comes from attribute declaration order, and
 * scalar type and conversion come from the attribute type. Adding a keyed layer would reintroduce
 * the drift failure mode a per-type map has elsewhere — an entry naming a type or attribute that no
 * longer exists, silently ignored at lookup rather than reported — for no benefit here, since
 * everything this class configures is already global.</p>
 *
 * <p>An empty configuration ({@code {}}) is behaviourally indistinguishable from RFC 4180 with no
 * configuration at all supplied — every default below is today's hard-coded behaviour, not a new
 * choice. That is what makes introducing this class a no-behaviour-change commit.</p>
 *
 * <p><b>Construction.</b> Two routes, both ending at the same constructor:</p>
 * <ul>
 *   <li>{@link #load(InputStream)}, for a deployment supplying a JSON document; and</li>
 *   <li>{@link #builder()}, for a Java caller using the serialiser directly — or
 *       {@link #EMPTY} for the defaults, and {@link #toBuilder()} to copy a configuration while
 *       varying one setting.</li>
 * </ul>
 * <p>Any setting left unset takes its default, so
 * {@code RosettaCSVConfiguration.builder().setHeaderStyle(HeaderStyle.LABEL).build()} differs from
 * {@link #EMPTY} in the header style alone. Pass the result to
 * {@code RosettaObjectMapperCreator.forCSV(RosettaCSVConfiguration, LabelProvider)}.</p>
 */
public class RosettaCSVConfiguration {
    public static final CsvDialect DEFAULT_DIALECT = CsvDialect.RFC_4180;
    public static final HeaderStyle DEFAULT_HEADER_STYLE = HeaderStyle.ATTRIBUTE_NAME;
    public static final String DEFAULT_LIST_DELIMITER = ";";
    /**
     * By default an empty cell, and only an empty cell, deserialises to an absent value.
     */
    public static final String DEFAULT_NULL_TOKEN = "";
    public static final boolean DEFAULT_HAS_HEADER = true;

    /**
     * The RFC 4180 / today's-hard-coded-behaviour configuration.
     */
    public static final RosettaCSVConfiguration EMPTY = builder().build();

    private final CsvDialect dialect;
    private final HeaderStyle headerStyle;
    private final String listDelimiter;
    private final String nullToken;
    private final boolean hasHeader;

    /**
     * Not public: construct through {@link #builder()} or {@link #load(InputStream)}. Jackson binds
     * to this constructor, so an absent JSON property arrives as {@code null} and takes its default
     * — which is what the boxed {@code Boolean} is for. A primitive {@code boolean} would bind an
     * absent {@code hasHeader} to {@code false}, quietly inverting the documented default and
     * making an empty configuration mean something other than today's behaviour.
     *
     * @param dialect       the file's punctuation — column delimiter, quote and escape character —
     *                      or {@code null} for {@link CsvDialect#RFC_4180}
     * @param headerStyle   what the header row holds: attribute names or labels. {@code null} for
     *                      {@link HeaderStyle#ATTRIBUTE_NAME}. {@link HeaderStyle#LABEL} additionally
     *                      requires a {@code LabelProvider}, which {@code RosettaCsvMapper} enforces
     *                      at its own construction rather than here.
     * @param listDelimiter the separator between the elements of a multi-cardinality attribute
     *                      serialised into one column, or {@code null} for {@code ;}
     * @param nullToken     the cell value that deserialises to an absent value, and that an absent
     *                      value is written back as, or {@code null} for the empty string.
     * @param hasHeader     whether the file being read (or, on write, the file being produced) has a
     *                      header row; {@code null} for {@code true}. This describes the file, not a
     *                      preference: a CSV file cannot be sniffed for a header — a header row of
     *                      codes looks like a data row — so it has to be declared, and this is where.
     *                      <b>Only {@code true} is currently supported</b> — see below.
     * @throws UnsupportedOperationException if {@code hasHeader} is {@code false}; header-less CSV
     *                                  is not implemented yet
     * @throws IllegalArgumentException if {@code listDelimiter} is the same as
     *                                  {@code dialect.columnDelimiter}
     */
    @JsonCreator
    private RosettaCSVConfiguration(
            @JsonProperty("dialect") CsvDialect dialect,
            @JsonProperty("headerStyle") HeaderStyle headerStyle,
            @JsonProperty("listDelimiter") String listDelimiter,
            @JsonProperty("nullToken") String nullToken,
            @JsonProperty("hasHeader") Boolean hasHeader) {
        this.dialect = dialect != null ? dialect : DEFAULT_DIALECT;
        this.headerStyle = headerStyle != null ? headerStyle : DEFAULT_HEADER_STYLE;
        this.listDelimiter = listDelimiter != null ? listDelimiter : DEFAULT_LIST_DELIMITER;
        this.nullToken = nullToken != null ? nullToken : DEFAULT_NULL_TOKEN;
        this.hasHeader = hasHeader != null ? hasHeader : DEFAULT_HAS_HEADER;
        if (!this.hasHeader) {
            // Rejected rather than accepted-and-ignored. Nothing on the read or write path honours
            // hasHeader=false today: the read schema is unconditionally withHeader(), so a
            // header-less file has its first data row consumed as column names and every later row
            // bound to bogus names and silently dropped; and the writer emits a header row
            // regardless. A setting that is accepted, has no effect and reports nothing is worse
            // than one that is refused.
            //
            // When header-less support lands, the check to reintroduce alongside it is
            // hasHeader=false with headerStyle=LABEL, which stays invalid on its own terms: a label
            // is header text, and a file declared to have no header row has nowhere to put one.
            throw new UnsupportedOperationException(
                    "RosettaCSVConfiguration: hasHeader=false is not implemented yet. Reading and writing "
                            + "header-less CSV is not supported — both paths assume a header row — so the setting "
                            + "is refused rather than accepted and silently ignored. Supply a file with a header "
                            + "row, or leave hasHeader at its default of true.");
        }
        if (this.listDelimiter.equals(String.valueOf(this.dialect.getColumnDelimiter()))) {
            throw new IllegalArgumentException(
                    "Invalid RosettaCSVConfiguration: listDelimiter '" + this.listDelimiter + "' is the same as "
                            + "dialect.columnDelimiter '" + this.dialect.getColumnDelimiter() + "'. A list element "
                            + "and a column boundary could not be told apart; choose a listDelimiter distinct from "
                            + "the column delimiter.");
        }
    }

    /**
     * A builder with every setting unset, so an un-customised {@code build()} yields a
     * configuration equal to {@link #EMPTY}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder pre-populated with this configuration's settings, for producing a copy that varies
     * one of them.
     */
    public Builder toBuilder() {
        return builder()
                .setDialect(dialect)
                .setHeaderStyle(headerStyle)
                .setListDelimiter(listDelimiter)
                .setNullToken(nullToken)
                .setHasHeader(hasHeader);
    }

    /**
     * Loads a configuration from JSON. An unknown property is tolerated, not rejected — mirroring
     * {@code RosettaXMLConfiguration.load}, so a configuration document written against a newer
     * version of this class still loads against an older one.
     *
     * <p>Every property is optional and an absent one takes its default, so {@code {}} loads to
     * {@link #EMPTY}. Unlike {@code RosettaXMLConfiguration}, this needs no {@code Jdk8Module}:
     * the constructor takes plain nullable types rather than {@code Optional}s, which also means a
     * caller's own {@code ObjectMapper} can deserialise this class without special configuration.</p>
     *
     * @param input the JSON document
     * @return the configuration the document describes, with defaults filled in
     * @throws IOException if the stream cannot be read or does not hold valid JSON
     */
    public static RosettaCSVConfiguration load(InputStream input) throws IOException {
        ObjectMapper csvConfigurationMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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

    /**
     * @return the cell value that deserialises to an absent value, and that an absent value is
     *         written back as. Defaults to the empty string, so by default only an empty cell is
     *         absent. A token that collides with a legitimate data value (e.g. a real attribute
     *         value of literally {@code "N/A"}) is indistinguishable from absence — that collision
     *         is the caller's problem to avoid by choosing a token that cannot occur in real data,
     *         not something this class can detect.
     *
     *         <p>To carry the empty string as data, set {@code nullToken} to something that cannot
     *         occur in the feed — whichever string is chosen becomes the one string that cannot
     *         then be data. There is no configuration under which every string is representable
     *         <em>and</em> absence is expressible; that is inherent to a token-based encoding of
     *         "absent", not a defect of this shape.</p>
     */
    public String getNullToken() {
        return nullToken;
    }

    /**
     * @return whether the file has a header row. Always {@code true} today — the constructor refuses
     *         {@code false} because header-less CSV is not implemented. The setting and this getter
     *         exist so the read and write paths can already be written against it.
     */
    public boolean isHasHeader() {
        return hasHeader;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dialect, headerStyle, listDelimiter, nullToken, hasHeader);
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
                && Objects.equals(nullToken, other.nullToken);
    }

    @Override
    public String toString() {
        return "RosettaCSVConfiguration{dialect=" + dialect + ", headerStyle=" + headerStyle
                + ", listDelimiter=" + listDelimiter + ", nullToken=" + nullToken + ", hasHeader=" + hasHeader + "}";
    }

    /**
     * Builds a {@link RosettaCSVConfiguration}. Any setting not set takes its default; there is no
     * order dependency between the setters, and the combination is validated once in
     * {@link #build()} rather than as each setter is called.
     */
    public static class Builder {
        private CsvDialect dialect;
        private HeaderStyle headerStyle;
        private String listDelimiter;
        private String nullToken;
        private Boolean hasHeader;

        private Builder() {
        }

        /**
         * @param dialect the file's punctuation — column delimiter, quote and escape character.
         *                Defaults to {@link CsvDialect#RFC_4180}. {@code null} restores that default.
         * @return this builder
         */
        public Builder setDialect(CsvDialect dialect) {
            this.dialect = dialect;
            return this;
        }

        /**
         * @param headerStyle what the header row holds: attribute names or labels. Defaults to
         *                    {@link HeaderStyle#ATTRIBUTE_NAME}. {@link HeaderStyle#LABEL}
         *                    additionally requires a {@code LabelProvider} — supplied to the mapper,
         *                    not to this configuration — and is incompatible with
         *                    {@code hasHeader=false}. {@code null} restores the default.
         * @return this builder
         */
        public Builder setHeaderStyle(HeaderStyle headerStyle) {
            this.headerStyle = headerStyle;
            return this;
        }

        /**
         * @param listDelimiter the separator between the elements of a multi-cardinality attribute
         *                      serialised into one column. Defaults to {@code ;}. {@code null}
         *                      restores that default.
         * @return this builder
         */
        public Builder setListDelimiter(String listDelimiter) {
            this.listDelimiter = listDelimiter;
            return this;
        }

        /**
         * @param nullToken the cell value that deserialises to an absent value, and that an absent
         *                  value is written back as. Defaults to the empty string, so by default
         *                  only an empty cell is absent. {@code null} restores that default — there
         *                  is no "disabled" value, since the empty string is itself a legitimate
         *                  token (the default), not an off switch. To carry the empty string as
         *                  data, set this to something that cannot occur in the feed.
         * @return this builder
         */
        public Builder setNullToken(String nullToken) {
            this.nullToken = nullToken;
            return this;
        }

        /**
         * @param hasHeader whether the file being read (or, on write, the file being produced) has a
         *                  header row. Defaults to {@code true}. This describes the file, not a
         *                  preference: a CSV file cannot be sniffed for a header — a header row of
         *                  codes looks like a data row — so it has to be declared, and this is where.
         *                  <b>Only {@code true} is currently supported</b>: header-less CSV is not
         *                  implemented, and {@link #build()} refuses {@code false} rather than
         *                  accepting a setting nothing acts on.
         * @return this builder
         */
        public Builder setHasHeader(boolean hasHeader) {
            this.hasHeader = hasHeader;
            return this;
        }

        /**
         * @return the configuration these settings describe, with defaults filled in
         * @throws UnsupportedOperationException if {@code hasHeader} is {@code false}; header-less
         *                                  CSV is not implemented yet
         * @throws IllegalArgumentException if {@code listDelimiter} is the same as the dialect's
         *                                  {@code columnDelimiter}
         */
        public RosettaCSVConfiguration build() {
            return new RosettaCSVConfiguration(dialect, headerStyle, listDelimiter, nullToken, hasHeader);
        }
    }
}
