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
import java.util.Optional;

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
 */
public class CsvDialect {
    public static final char DEFAULT_COLUMN_DELIMITER = ',';
    public static final char DEFAULT_QUOTE_CHAR = '"';
    public static final char DEFAULT_ESCAPE_CHAR = '"';

    /**
     * The RFC 4180 dialect: comma-separated, double-quoted, doubled-quote escaping.
     */
    public static final CsvDialect RFC_4180 = new CsvDialect(Optional.empty(), Optional.empty(), Optional.empty());

    private final char columnDelimiter;
    private final char quoteChar;
    private final char escapeChar;

    @JsonCreator
    public CsvDialect(
            @JsonProperty("columnDelimiter") Optional<Character> columnDelimiter,
            @JsonProperty("quoteChar") Optional<Character> quoteChar,
            @JsonProperty("escapeChar") Optional<Character> escapeChar) {
        this.columnDelimiter = columnDelimiter.orElse(DEFAULT_COLUMN_DELIMITER);
        this.quoteChar = quoteChar.orElse(DEFAULT_QUOTE_CHAR);
        this.escapeChar = escapeChar.orElse(DEFAULT_ESCAPE_CHAR);
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
}
