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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.regnosys.rosetta.common.serialisation.RosettaCsvMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.csv.config.HeaderStyle;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import com.rosetta.model.lib.functions.LabelProvider;
import csv.test.multi.MultiCardinalityAttributes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Write-side tests (TASK-9540 session 1; see {@code csv-single-delimited-column-lists.md} §4.1) and
 * read-side / round-trip tests (session 2; §4.2) for a multi-cardinality simple attribute serialising
 * into a single delimited CSV column.
 *
 * <p>{@code MultiCardinalityAttributes} ({@code csv.test.multi}) declares {@code id (1..1)} then
 * {@code tags (0..*)}, so column order alone would catch a delimiter wired to the wrong schema.
 *
 * <p>The read mechanism needs no extra wiring beyond what session 1 already put in place: every
 * schema this class builds — read or write, plain, labelled or positional-fallback — passes through
 * the same {@code dialectSchema} helper, which stamps {@code listDelimiter} on as the schema-wide
 * array-element separator. On read, jackson-dataformat-csv's {@code CsvParser.isExpectedStartArrayToken()}
 * then coerces an untyped (STRING) column's cell into array elements whenever the target bean
 * property is a {@code Collection}, independently of the column's declared type — the read-side twin
 * of the write-side finding in session 1 that the separator applies per schema, not per column.
 * Measured directly against this mapper, not inferred from the jackson source.
 */
public class RosettaCsvMapperListColumnTest {

    private static MultiCardinalityAttributes withTags(String... tags) {
        return MultiCardinalityAttributes.builder()
                .setId("id1")
                .addTags(Arrays.asList(tags))
                .build();
    }

    @Test
    void listWritesAsOneCellJoinedByTheDefaultListDelimiter() throws JsonProcessingException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        String csv = mapper.writeValueAsString(withTags("EUR", "USD", "GBP"));
        assertEquals("id,tags\nid1,EUR;USD;GBP\n", csv);
    }

    @Test
    void singleElementListWritesWithNoDelimiter() throws JsonProcessingException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        String csv = mapper.writeValueAsString(withTags("EUR"));
        assertEquals("id,tags\nid1,EUR\n", csv);
    }

    /**
     * The generated immutable object stores an empty list exactly as it stores an absent one — both
     * collapse to {@code null} in the builder (a list attribute is only ever non-null when it holds
     * at least one element) — so there is no distinct "empty list" value for the writer to receive in
     * the first place. Both therefore write the same empty cell; the two cases cannot differ, and
     * this test documents that measurement rather than asserting a choice.
     */
    @Test
    void anEmptyListAndAnAbsentListBothWriteAnEmptyCell() throws JsonProcessingException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes absent = MultiCardinalityAttributes.builder().setId("id1").build();
        MultiCardinalityAttributes empty = MultiCardinalityAttributes.builder()
                .setId("id1")
                .setTags(Collections.emptyList())
                .build();

        String expected = "id,tags\nid1,\n";
        assertEquals(expected, mapper.writeValueAsString(absent));
        assertEquals(expected, mapper.writeValueAsString(empty));
    }

    @Test
    void listElementContainingTheColumnDelimiterIsQuoted() throws JsonProcessingException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        String csv = mapper.writeValueAsString(withTags("EUR,X", "USD"));
        assertEquals("id,tags\nid1,\"EUR,X;USD\"\n", csv);
    }

    @Test
    void configuredListDelimiterIsUsedInsteadOfTheDefault() throws JsonProcessingException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder().setListDelimiter("|").build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        String csv = mapper.writeValueAsString(withTags("EUR", "USD", "GBP"));
        assertEquals("id,tags\nid1,EUR|USD|GBP\n", csv);
    }

    @Test
    void listElementContainingTheListDelimiterIsRejectedAtWriteTime() {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes value = withTags("EUR;X", "USD");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("tags"));
        assertTrue(exception.getMessage().contains("EUR;X"));
    }

    @Test
    void listElementContainingAConfiguredNonDefaultListDelimiterIsRejectedAtWriteTime() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder().setListDelimiter("|").build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        MultiCardinalityAttributes value = withTags("EUR|X", "USD");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("tags"));
        assertTrue(exception.getMessage().contains("EUR|X"));
    }

    /**
     * The writer must not be able to produce a file this mapper's own reader refuses. An element
     * that is the empty string joins into a cell with a trailing or doubled list delimiter, which
     * the read side rejects (see {@code aTrailingListDelimiterIsRejectedAtReadTime…} below) because
     * jackson reads such an element back as a Java {@code null} that a list attribute cannot hold.
     * Rejecting it on write too puts the failure at the point the bad value is known — the same
     * choice already made for an element containing the list delimiter.
     */
    @Test
    void listElementEqualToTheDefaultNullTokenIsRejectedAtWriteTime() {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes value = withTags("EUR", "");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("tags"));
        assertTrue(exception.getMessage().contains("null token"));
    }

    @Test
    void listElementEqualToAConfiguredNullTokenIsRejectedAtWriteTime() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Collections.singletonList("N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        MultiCardinalityAttributes value = withTags("EUR", "N/A");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("tags"));
        assertTrue(exception.getMessage().contains("N/A"));
    }

    /**
     * With no null tokens configured there is nothing for an empty element to be confused with, so
     * the write side must not reject it either — and the pair round-trips. This is the write-side
     * half of {@code aTrailingListDelimiterProducesAnEmptyElementWhenNoNullTokensAreConfigured}.
     */
    @Test
    void anEmptyListElementRoundTripsWhenNoNullTokensAreConfigured() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Collections.emptyList())
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        MultiCardinalityAttributes original = withTags("EUR", "");

        String csv = mapper.writeValueAsString(original);

        assertEquals("id,tags\nid1,EUR;\n", csv);
        assertEquals(original, mapper.readValue(csv, MultiCardinalityAttributes.class));
    }

    /**
     * A scalar attribute equal to a null token is <i>not</i> rejected: it round-trips as an absent
     * value, which is exactly what a null token is for. Only a <i>list element</i> is unrepresentable,
     * because a list cannot hold a null.
     */
    @Test
    void aScalarAttributeEqualToTheNullTokenIsUnaffected() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes value = MultiCardinalityAttributes.builder()
                .setId("")
                .addTags(Collections.singletonList("EUR"))
                .build();

        String csv = mapper.writeValueAsString(value);

        assertEquals("id,tags\n,EUR\n", csv);
    }

    /**
     * A value containing the list delimiter but written to a plain (non-list) column must not be
     * rejected — the delimiter has no special meaning there, only within a multi-cardinality column.
     */
    @Test
    void aScalarColumnValueContainingTheListDelimiterIsUnaffected() throws JsonProcessingException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes value = MultiCardinalityAttributes.builder()
                .setId("id;1")
                .addTags(Collections.singletonList("EUR"))
                .build();
        String csv = mapper.writeValueAsString(value);
        assertEquals("id,tags\nid;1,EUR\n", csv);
    }

    // ---------------------------------------------------------------------------
    // Session 2 — read side and round trip
    // ---------------------------------------------------------------------------

    @Test
    void roundTripsAPopulatedListOnThePlainPath() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes original = withTags("EUR", "USD", "GBP");

        String csv = mapper.writeValueAsString(original);
        MultiCardinalityAttributes roundTripped = mapper.readValue(csv, MultiCardinalityAttributes.class);

        assertEquals(original, roundTripped);
    }

    @Test
    void roundTripsAPopulatedListOnTheLabelledPath() throws IOException {
        Map<String, String> labels = new HashMap<>();
        labels.put("id", "ID");
        labels.put("tags", "Tags");
        LabelProvider provider = path -> labels.get(path.buildPath());
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);
        MultiCardinalityAttributes original = withTags("EUR", "USD", "GBP");

        String csv = mapper.writeValueAsString(original);
        MultiCardinalityAttributes roundTripped = mapper.readValue(csv, MultiCardinalityAttributes.class);

        assertEquals(original, roundTripped);
    }

    /**
     * Duplicate labels force positional binding ({@code RosettaCsvMapperLabelledTest} exercises that
     * fallback directly); this only needs to confirm a list column survives it too.
     */
    @Test
    void roundTripsAPopulatedListThroughThePositionalFallback() throws IOException {
        Map<String, String> labels = new HashMap<>();
        labels.put("id", "Name");
        labels.put("tags", "Name");
        LabelProvider provider = path -> labels.get(path.buildPath());
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);
        MultiCardinalityAttributes original = withTags("EUR", "USD", "GBP");

        String csv = mapper.writeValueAsString(original);
        MultiCardinalityAttributes roundTripped = mapper.readValue(csv, MultiCardinalityAttributes.class);

        assertEquals(original, roundTripped);
    }

    @Test
    void oneCellDeserialisesToAMultiElementList() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,EUR;USD;GBP\n", MultiCardinalityAttributes.class);
        assertEquals(withTags("EUR", "USD", "GBP"), result);
    }

    @Test
    void aCellWithNoDelimiterDeserialisesToASingleElementList() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,EUR\n", MultiCardinalityAttributes.class);
        assertEquals(withTags("EUR"), result);
    }

    /**
     * Measured (see {@code csv-single-delimited-column-lists.md} §3): a wholly empty cell matches the
     * default null token before array-splitting is even considered, so the property comes back
     * {@code null} rather than a one-element list holding {@code ""}. This is not distinguishable from
     * "empty list" for a Rosetta model object anyway — session 1 established that the generated
     * immutable object already collapses an empty list and an absent one to the same {@code null}
     * {@code getTags()} — so asserting equality against the absent-list object is the whole story,
     * not a gap.
     */
    @Test
    void anEmptyCellDeserialisesToTheSameValueAsAnAbsentList() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,\n", MultiCardinalityAttributes.class);

        MultiCardinalityAttributes absent = MultiCardinalityAttributes.builder().setId("id1").build();
        MultiCardinalityAttributes empty = MultiCardinalityAttributes.builder()
                .setId("id1")
                .setTags(Collections.emptyList())
                .build();
        assertEquals(absent, result);
        assertEquals(empty, result);
    }

    /**
     * {@code TRIM_SPACES} is off and nothing in this mapper turns it on (§3 of the plan) — a
     * deliberate default, not an oversight, so it gets a regression test rather than silent drift.
     */
    @Test
    void elementWhitespaceIsPreserved() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,a; b\n", MultiCardinalityAttributes.class);
        assertEquals(withTags("a", " b"), result);
    }

    @Test
    void aQuotedCellContainingTheColumnDelimiterDeserialisesToOneListElement() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        MultiCardinalityAttributes result = mapper.readValue(
                "id,tags\nid1,\"EUR,X;USD\"\n", MultiCardinalityAttributes.class);
        assertEquals(withTags("EUR,X", "USD"), result);
    }

    @Test
    void configuredListDelimiterRoundTripsOnRead() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder().setListDelimiter("|").build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,EUR|USD|GBP\n", MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR", "USD", "GBP"), result);
    }

    /**
     * A trailing (or doubled) list delimiter splits into an element that is the empty string —
     * indistinguishable, under the default {@code nullTokens}, from the token that means "absent".
     * Jackson applies that null-token comparison per split element as well as to the whole cell (the
     * mechanism behind the previous test), so left alone this fails five stack frames deep in a
     * {@code NullPointerException} thrown by Guava's null-rejecting {@code ImmutableList}, wrapped
     * twice by jackson. There is no producer that means to send an empty tag, so the decision here is
     * to reject on read, the same choice session 1 made for a list element containing the list
     * delimiter on write — loud and named, rather than a low-level exception with no attribute or
     * cell attached, or (the alternative) silently keeping the empty string as a real element.
     */
    @Test
    void aTrailingListDelimiterIsRejectedAtReadTimeRatherThanProducingAnUnexplainedFailure() {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.readValue("id,tags\nid1,EUR;\n", MultiCardinalityAttributes.class));
        assertTrue(exception.getMessage().contains("tags"));
        assertTrue(exception.getMessage().contains("EUR;"));
    }

    /**
     * The same collision, forced without a trailing delimiter: a doubled delimiter produces an empty
     * element in the middle of the cell, not just at the end.
     */
    @Test
    void aDoubledListDelimiterIsRejectedAtReadTime() {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.readValue("id,tags\nid1,EUR;;USD\n", MultiCardinalityAttributes.class));
        assertTrue(exception.getMessage().contains("tags"));
    }

    /**
     * The null-token collision check needs the header row to know which column binds to which
     * attribute, but it must not read it before deciding whether it has anything to check — and it
     * must not turn an empty document into a labelled-CSV error on a read that is not labelled.
     * Empty content on the plain path therefore surfaces jackson's own failure ({@code
     * CsvReadException: Empty header line}), for a type with a list attribute (the check runs and
     * finds no rows) as much as for one without (the check returns before reading anything).
     */
    @Test
    void emptyContentOnThePlainPathReportsAJacksonErrorRatherThanAMissingLabelHeader() {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        JsonMappingException exception = assertThrows(JsonMappingException.class,
                () -> mapper.readValue("", MultiCardinalityAttributes.class));

        assertTrue(exception.getMessage().contains("Empty header line"), exception.getMessage());
        assertFalse(exception.getMessage().contains("labelled"), exception.getMessage());
    }

    /**
     * The labelled path keeps the opposite behaviour: it genuinely cannot proceed without a header,
     * so it still fails with its own named error rather than deferring to jackson.
     */
    @Test
    void emptyContentOnTheLabelledPathStillReportsAMissingHeaderRow() {
        LabelProvider provider = path -> null;
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mapper.readValue("", MultiCardinalityAttributes.class));

        assertTrue(exception.getMessage().contains("missing header row"));
    }

    /**
     * With no configured null tokens there is nothing for an empty element to collide with, so the
     * same trailing-delimiter cell reads back as a genuine empty-string element instead of throwing.
     */
    @Test
    void aTrailingListDelimiterProducesAnEmptyElementWhenNoNullTokensAreConfigured() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Collections.emptyList())
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,EUR;\n", MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR", ""), result);
    }
}
