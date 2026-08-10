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

import java.util.Objects;

/**
 * The punctuation of a CSV file: the character separating columns, the character quoting a
 * value that contains that separator, and the character escaping a quote inside a quoted value.
 *
 * <p>{@code columnDelimiter}, {@code quoteChar} and {@code escapeChar} are a single {@code char}
 * each, not a {@code String} — {@code com.fasterxml.jackson.dataformat.csv.CsvSchema} takes a
 * {@code char} for each of these three. (The list delimiter on {@code RosettaCSVConfiguration} is
 * a {@code String}, because {@code CsvSchema}'s array-element separator is.)</p>
 *
 * <p>Defaults are RFC 4180: comma-separated, double-quoted, with a quote inside a quoted value
 * escaped by doubling it. That last rule is why {@link #getEscapeChar()} defaults to the same
 * character as {@link #getQuoteChar()} rather than to a distinct escape character such as
 * {@code \} — RFC 4180 has no separate escape character, only the doubled quote. Translating that
 * into a {@code CsvSchema} (an explicit escape character vs. {@code withoutEscapeChar()}) is the
 * mapper's job, not this class's.</p>
 *
 * <p><b>Construction.</b> Use {@link #builder()}, or {@link #RFC_4180} for the defaults. Every
 * setting left unset on the builder takes its default, so
 * {@code CsvDialect.builder().setColumnDelimiter(';').build()} is a semicolon-separated RFC 4180
 * dialect in every other respect. JSON deserialisation goes through the same constructor, so a
 * document naming only {@code columnDelimiter} produces the same object.</p>
 */
public class CsvDialect {
    public static final char DEFAULT_COLUMN_DELIMITER = ',';
    public static final char DEFAULT_QUOTE_CHAR = '"';
    public static final char DEFAULT_ESCAPE_CHAR = '"';

    /**
     * The RFC 4180 dialect: comma-separated, double-quoted, doubled-quote escaping.
     */
    public static final CsvDialect RFC_4180 = builder().build();

    private final char columnDelimiter;
    private final char quoteChar;
    private final char escapeChar;

    /**
     * Not public: construct through {@link #builder()}. Jackson binds to this constructor, so an
     * absent JSON property arrives as {@code null} and takes its default — which is what the boxed
     * parameter types are for. A primitive {@code char} would bind an absent property to NUL
     * instead, silently replacing the RFC 4180 defaults with unusable ones.
     *
     * @param columnDelimiter the character separating columns, or {@code null} for a comma
     * @param quoteChar       the character quoting a value that contains the column delimiter, a
     *                        newline or a quote, or {@code null} for a double quote
     * @param escapeChar      the character escaping a quote inside a quoted value, or {@code null}
     *                        for a double quote — RFC 4180's doubled quote, not a distinct escape
     */
    @JsonCreator
    private CsvDialect(
            @JsonProperty("columnDelimiter") Character columnDelimiter,
            @JsonProperty("quoteChar") Character quoteChar,
            @JsonProperty("escapeChar") Character escapeChar) {
        this.columnDelimiter = columnDelimiter != null ? columnDelimiter : DEFAULT_COLUMN_DELIMITER;
        this.quoteChar = quoteChar != null ? quoteChar : DEFAULT_QUOTE_CHAR;
        this.escapeChar = escapeChar != null ? escapeChar : DEFAULT_ESCAPE_CHAR;
    }

    /**
     * A builder with every setting unset, so an un-customised {@code build()} yields a dialect
     * equal to {@link #RFC_4180}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder pre-populated with this dialect's settings, for producing a copy that varies one
     * of them.
     */
    public Builder toBuilder() {
        return builder()
                .setColumnDelimiter(columnDelimiter)
                .setQuoteChar(quoteChar)
                .setEscapeChar(escapeChar);
    }

    public char getColumnDelimiter() {
        return columnDelimiter;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

    public char getEscapeChar() {
        return escapeChar;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnDelimiter, quoteChar, escapeChar);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CsvDialect other = (CsvDialect) obj;
        return columnDelimiter == other.columnDelimiter
                && quoteChar == other.quoteChar
                && escapeChar == other.escapeChar;
    }

    @Override
    public String toString() {
        return "CsvDialect{columnDelimiter=" + columnDelimiter + ", quoteChar=" + quoteChar + ", escapeChar=" + escapeChar + "}";
    }

    /**
     * Builds a {@link CsvDialect}. Any setting not set takes its RFC 4180 default; there is no
     * order dependency between the setters.
     */
    public static class Builder {
        private Character columnDelimiter;
        private Character quoteChar;
        private Character escapeChar;

        private Builder() {
        }

        /**
         * @param columnDelimiter the character separating columns. Defaults to a comma.
         * @return this builder
         */
        public Builder setColumnDelimiter(char columnDelimiter) {
            this.columnDelimiter = columnDelimiter;
            return this;
        }

        /**
         * @param quoteChar the character quoting a value that contains the column delimiter, a
         *                  newline or a quote. Defaults to a double quote.
         * @return this builder
         */
        public Builder setQuoteChar(char quoteChar) {
            this.quoteChar = quoteChar;
            return this;
        }

        /**
         * @param escapeChar the character escaping a quote inside a quoted value. Defaults to a
         *                   double quote, RFC 4180's doubled-quote rule rather than a distinct
         *                   escape character such as {@code \}.
         * @return this builder
         */
        public Builder setEscapeChar(char escapeChar) {
            this.escapeChar = escapeChar;
            return this;
        }

        public CsvDialect build() {
            return new CsvDialect(columnDelimiter, quoteChar, escapeChar);
        }
    }
}
