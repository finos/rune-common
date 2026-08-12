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
import csv.test.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Write-side, read-side and round-trip tests for a multi-cardinality simple attribute serialising into
 * a single delimited CSV column.
 *
 * <p>{@code MultiCardinalityAttributes} ({@code csv.test.multi}) declares {@code id (1..1)} then
 * {@code tags (0..*)}, so column order alone would catch a delimiter wired to the wrong schema.
 *
 * <p>Both directions run off one mechanism: every schema this mapper builds — read or write, plain,
 * labelled or positional-fallback — passes through {@code dialectSchema}, which stamps
 * {@code listDelimiter} on as the schema-wide array-element separator. On write, jackson's
 * {@code CsvGenerator} recognises a multi-valued property from the {@code writeStartArray()} calls the
 * bean serialiser makes and joins with that separator; on read,
 * {@code CsvParser.isExpectedStartArrayToken()} splits an untyped (STRING) column's cell into elements
 * whenever the target bean property is a {@code Collection}. Both are independent of the column's
 * declared type, and both were measured directly against this mapper rather than inferred from the
 * jackson source.
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

    /**
     * A list column takes the null token on write exactly as a scalar column does, rather than keeping
     * the blank cell it wrote before {@code nullToken} was honoured. Decided rather than inherited: the
     * alternative — an absent scalar writing {@code N/A} while an absent list writes blank — would mean
     * two spellings of absence in one row, and the blank one is the spelling that reads back as the
     * empty string under this very configuration.
     *
     * <p>It round-trips because a whole cell equal to the token already deserialises to an absent list
     * (see {@code aWholeCellEqualToANullTokenStillMeansAnAbsentList}). The empty-list case is not a
     * second behaviour: the generated builder collapses an empty list to an absent one, so the writer
     * never receives a distinct value — the same measurement
     * {@code anEmptyListAndAnAbsentListBothWriteAnEmptyCell} records for the default token.</p>
     */
    @Test
    void anAbsentListWritesTheConfiguredNullTokenAndRoundTrips() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        MultiCardinalityAttributes absent = MultiCardinalityAttributes.builder().setId("id1").build();
        MultiCardinalityAttributes empty = MultiCardinalityAttributes.builder()
                .setId("id1")
                .setTags(Collections.emptyList())
                .build();

        String csv = mapper.writeValueAsString(absent);

        assertEquals("id,tags\nid1,N/A\n", csv);
        assertEquals(csv, mapper.writeValueAsString(empty));
        assertEquals(absent, mapper.readValue(csv, MultiCardinalityAttributes.class));
    }

    /**
     * A populated list is untouched by the write-side null token: only an absent value takes the token,
     * so a non-empty list still writes its delimited elements.
     */
    @Test
    void aPopulatedListIsUnaffectedByANonEmptyNullToken() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        MultiCardinalityAttributes original = withTags("EUR", "USD");

        String csv = mapper.writeValueAsString(original);

        assertEquals("id,tags\nid1,EUR;USD\n", csv);
        assertEquals(original, mapper.readValue(csv, MultiCardinalityAttributes.class));
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
     * The reader drops a list element equal to a null token (see
     * {@code aTrailingListDelimiterHasItsEmptyElementDropped} below), so the writer must refuse to
     * emit one: otherwise the value would come back one element shorter, silently, and for a
     * {@code (1..*)} attribute that is a cardinality violation rather than merely a different string.
     * Loud on write, forgiving on read — the asymmetry is the point, and the failure lands where the
     * bad value is known.
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
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        MultiCardinalityAttributes value = withTags("EUR", "N/A");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("tags"));
        assertTrue(exception.getMessage().contains("N/A"));
    }

    /**
     * With the null token reconfigured away from the default, there is nothing for an empty element
     * to be confused with, so the write side must not reject it either — and the pair round-trips.
     * This is the write-side half of
     * {@code aTrailingListDelimiterProducesAnEmptyElementWhenTheNullTokenIsNotEmpty}.
     */
    @Test
    void anEmptyListElementRoundTripsWhenTheNullTokenIsNotEmpty() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
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
     * A wholly empty cell matches the default null token before array-splitting is even considered, so
     * the property comes back {@code null} rather than a one-element list holding {@code ""}. That is
     * indistinguishable from "empty list" for a Rosetta model object regardless: the generated immutable
     * collapses an empty list and an absent one to the same {@code null} {@code getTags()}, so asserting
     * equality against the absent-list object is the whole story, not a gap.
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
     * {@code TRIM_SPACES} is off and nothing in this mapper turns it on — a deliberate default, not an
     * oversight, so it gets a regression test rather than silent drift.
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
     * indistinguishable, under the default {@code nullToken}, from the token that means "absent". Rune
     * cannot represent that: {@code empty} is the absence of a value, and for a multi-valued attribute
     * it means the empty <i>list</i>, never a hole inside one (the generated immutable rejects a
     * {@code null} element, and {@code MapperC} filters one out as an error item). So an element that
     * means "absent" is <b>dropped</b> — the direct analogue of the scalar rule that a cell meaning
     * "absent" makes its attribute absent.
     */
    @Test
    void aTrailingListDelimiterHasItsEmptyElementDropped() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,EUR;\n", MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR"), result);
    }

    /**
     * The same case forced without a trailing delimiter: a doubled delimiter produces an empty element
     * in the middle of the cell, not just at the end, and the elements around it keep their order.
     */
    @Test
    void aDoubledListDelimiterHasItsEmptyElementDropped() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        MultiCardinalityAttributes result = mapper.readValue(
                "id,tags\nid1,EUR;;USD\n", MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR", "USD"), result);
    }

    /**
     * A cell of nothing but delimiters loses every element, leaving the attribute absent — the same
     * value an empty cell gives.
     */
    @Test
    void aCellOfOnlyDelimitersDeserialisesToAnAbsentList() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,\";;\"\n", MultiCardinalityAttributes.class);

        assertEquals(MultiCardinalityAttributes.builder().setId("id1").build(), result);
    }

    /**
     * The configured null token is dropped from a list cell exactly as it is from a scalar cell — a
     * non-empty token inside a list cell means absent exactly as the empty one does.
     */
    @Test
    void anElementEqualToANonEmptyNullTokenIsDropped() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue(
                "id,tags\nid1,EUR;N/A\n", MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR"), result);
    }

    /**
     * A list cell whose every element is a null token leaves the attribute absent — the same value an
     * empty cell gives. Uses a non-empty token, so the stripped cell is genuinely empty rather than
     * merely unchanged.
     */
    @Test
    void aListCellOfOnlyNullTokensDeserialisesToAnAbsentList() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue(
                "id,tags\nid1,N/A;N/A\n", MultiCardinalityAttributes.class);

        assertEquals(MultiCardinalityAttributes.builder().setId("id1").build(), result);
    }

    /**
     * A cell holding a single element is the whole-cell case and belongs to the schema's own null-value
     * handling, so the element-stripping pre-pass must leave it alone: a cell that is exactly a null
     * token still means "absent attribute". Uses a non-empty token, the configuration where stripping it
     * here instead would change the bytes jackson sees.
     */
    @Test
    void aWholeCellEqualToANullTokenStillMeansAnAbsentList() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue(
                "id,tags\nid1,N/A\n", MultiCardinalityAttributes.class);

        assertEquals(MultiCardinalityAttributes.builder().setId("id1").build(), result);
    }

    /**
     * {@code readValue(URL, …)} hands the document straight to jackson when nothing needs the whole
     * file up front. The element-stripping pre-pass does need it, for a type with a list attribute and
     * at least one null token configured — so that path has to stop streaming, or the cell reaches
     * jackson unstripped and fails inside Guava's null-rejecting {@code ImmutableList} instead. A
     * behaviour that differs between the {@code String} and {@code URL} overloads of the same read is
     * the kind of gap only a test at the boundary catches.
     */
    @Test
    void aTrailingListDelimiterIsAlsoStrippedWhenReadFromAUrl(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("tags.csv");
        Files.write(file, "id,tags\nid1,EUR;\n".getBytes(StandardCharsets.UTF_8));
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        MultiCardinalityAttributes result = mapper.readValue(file.toUri().toURL(), MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR"), result);
    }

    /**
     * A type of scalars only keeps streaming — the pre-pass has nothing to strip without a list
     * attribute, so the {@code URL} overload must not start buffering for it.
     */
    @Test
    void aTypeWithNoListAttributeStillReadsFromAUrl(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("user.csv");
        Files.write(file, "username,identifier,firstName,lastName\nuser1,id1,First1,Last1\n"
                .getBytes(StandardCharsets.UTF_8));
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        User result = mapper.readValue(file.toUri().toURL(), User.class);

        assertEquals("user1", result.getUsername());
        assertEquals("Last1", result.getLastName());
    }

    /**
     * The element-stripping pre-pass needs the header row to know which column binds to which
     * attribute, but it must not read it before deciding whether it has anything to strip — and it
     * must not turn an empty document into a labelled-CSV error on a read that is not labelled.
     * Empty content on the plain path therefore surfaces jackson's own failure ({@code
     * CsvReadException: Empty header line}), for a type with a list attribute (the pre-pass runs and
     * finds no rows) as much as for one without (the pre-pass returns before reading anything).
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
     * With the null token reconfigured away from the default, there is nothing for an empty element
     * to collide with, so the same trailing-delimiter cell reads back as a genuine empty-string
     * element instead of being dropped.
     */
    @Test
    void aTrailingListDelimiterProducesAnEmptyElementWhenTheNullTokenIsNotEmpty() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        MultiCardinalityAttributes result = mapper.readValue("id,tags\nid1,EUR;\n", MultiCardinalityAttributes.class);

        assertEquals(withTags("EUR", ""), result);
    }
}
