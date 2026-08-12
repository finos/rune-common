package com.regnosys.rosetta.common.serialisation.csv;

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

import com.regnosys.rosetta.common.serialisation.RosettaCsvMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import csv.test.multi.MultiCardinalityAttributes;
import csv.test.nullable.NullableAttributes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@code nullToken}: the cell value that deserialises to an absent attribute, and that an
 * absent attribute writes back as.
 *
 * <p>Uses the generated {@code NullableAttributes} type ({@code id (1..1)}, {@code note (0..1)}) —
 * neither existing test type ({@code User}, {@code LabelledTrade}) declares an optional attribute,
 * and one is needed to observe absent-vs-empty-string at all.</p>
 */
public class RosettaCsvMapperNullTokensTest {

    @Test
    void defaultConfigurationEmptyCellDeserialisesToAbsent() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        NullableAttributes result = mapper.readValue("id,note\nabc,\n", NullableAttributes.class);

        assertEquals("abc", result.getId());
        assertNull(result.getNote());
    }

    @Test
    void defaultConfigurationAbsentAttributeWritesAsEmptyCell() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        NullableAttributes noNote = NullableAttributes.builder().setId("abc").build();

        String written = mapper.writeValueAsString(noNote);

        assertEquals("id,note\nabc,\n", written);
    }

    /**
     * The documented answer for carrying the empty string as data: reconfigure {@code nullToken} to
     * something that cannot occur in the feed, and a blank cell is no longer coerced to absent.
     */
    @Test
    void aNonEmptyNullTokenLeavesTheEmptyStringAsData() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        NullableAttributes result = mapper.readValue("id,note\nabc,\n", NullableAttributes.class);

        assertEquals("", result.getNote());
    }

    @Test
    void configuredNullTokenDeserialisesToAbsent() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        assertNull(mapper.readValue("id,note\nabc,N/A\n", NullableAttributes.class).getNote());
    }

    @Test
    void configuredNullTokenLeavesANonMatchingValueUntouched() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        NullableAttributes result = mapper.readValue("id,note\nabc,a real value\n", NullableAttributes.class);

        assertEquals("a real value", result.getNote());
    }

    /**
     * A configured null token in a middle column routes the document through the list-stripping
     * pre-pass ({@code stripListElementsMeaningAbsent}), and that pre-pass must not change the row's
     * arity. It used to be able to: it substituted a Java {@code null}, and jackson's CSV generator
     * omits a null element from the column-less schema the pre-pass writes with, rather than emitting
     * an empty field — so the column disappeared and every column after it shifted one place left,
     * binding each attribute to its neighbour's value. Silent, and plausible rather than obviously
     * wrong.
     *
     * <p>{@code MultiCardinalityAttributes} is used — rather than a scalar-only type — because a
     * scalar-only type never enters this pre-pass at all: {@code stripListElementsMeaningAbsent}
     * returns its input unchanged whenever the type has no multi-cardinality attribute, before it
     * reads a single row.</p>
     */
    @Test
    void aNullTokenInAMiddleColumnDoesNotShiftTheColumnsAfterIt() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\nN/A,EUR\n", MultiCardinalityAttributes.class);

        MultiCardinalityAttributes expected = MultiCardinalityAttributes.builder()
                .addTags(Collections.singletonList("EUR"))
                .build();
        assertEquals(expected, result);
    }

    /**
     * The same shift, from the other direction: this cell matches no <i>configured</i> token beyond
     * the default — it is simply empty — but the pre-pass's raw reader maps a cell equal to the
     * schema's null value to Java {@code null} just the same, so it was dropped on rewrite just the
     * same. Any absent cell outside the last column was affected, not only one spelt with a
     * non-default token.
     */
    @Test
    void anEmptyCellInAMiddleColumnDoesNotShiftTheColumnsAfterIt() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\n,EUR\n", MultiCardinalityAttributes.class);

        MultiCardinalityAttributes expected = MultiCardinalityAttributes.builder()
                .addTags(Collections.singletonList("EUR"))
                .build();
        assertEquals(expected, result);
    }

    /**
     * The list-stripping pre-pass re-renders the whole document once it strips anything, so a quoted
     * cell elsewhere in the row has to survive it byte-for-byte — including one holding the column
     * delimiter, which is the case that would break if the rewrite ever stopped going through the
     * mapper's own dialect-aware writer.
     */
    @Test
    void aQuotedCellSurvivesTheListStrippingRewrite() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue(
                "id,tags\n\"First,Name\",EUR;N/A\n", MultiCardinalityAttributes.class);

        MultiCardinalityAttributes expected = MultiCardinalityAttributes.builder()
                .setId("First,Name")
                .addTags(Collections.singletonList("EUR"))
                .build();
        assertEquals(expected, result);
    }

    /**
     * With one token there is no "canonical" token to write back that differs from the one configured
     * — the default is used for both directions, so the round trip is trivially symmetric.
     */
    @Test
    void roundTripThroughAFileContainingTheDefaultNullTokenWritesBackTheSameToken() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        NullableAttributes read = mapper.readValue("id,note\nabc,\n", NullableAttributes.class);
        assertNull(read.getNote());

        String written = mapper.writeValueAsString(read);

        assertEquals("id,note\nabc,\n", written);
    }

    /**
     * <b>Known gap, closed by TASK-9623 session 6.6.</b> Under a non-empty {@code nullToken}, an
     * absent optional attribute still writes a <i>blank</i> cell rather than the configured token —
     * so this file, read back through the same configuration, deserialises {@code note} as the empty
     * string rather than absent. The mapper's own writer produces a file its own reader misreads,
     * under precisely the configuration {@code nullToken}'s javadoc recommends for carrying the empty
     * string as data. See {@code csv-single-delimited-column-lists.md} §6.2 and
     * {@code csv-single-null-token.md} §4.
     */
    @Test
    void anAbsentAttributeStillWritesABlankCellEvenWhenTheNullTokenIsNotEmpty() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes noNote = NullableAttributes.builder().setId("abc").build();

        String written = mapper.writeValueAsString(noNote);

        assertEquals("id,note\nabc,\n", written);
    }
}
