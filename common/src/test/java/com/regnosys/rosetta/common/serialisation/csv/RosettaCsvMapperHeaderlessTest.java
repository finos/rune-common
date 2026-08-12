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
import com.regnosys.rosetta.common.serialisation.csv.config.CsvDialect;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import csv.test.multi.MultiCardinalityAttributes;
import csv.test.nullable.NullableAttributes;
import csv.test.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading and writing CSV with no header row — {@code hasHeader: false}.
 *
 * <p>Before this, {@code hasHeader=false} was refused at construction, and the underlying read path
 * lost data silently: the schema was unconditionally {@code withHeader()}, so a header-less file had
 * its first data row consumed as column names and every later row bound to names no attribute
 * matched, then dropped without a word because {@code FAIL_ON_UNKNOWN_PROPERTIES} is disabled. The
 * measured result was an all-null object. See the story plan's §3.3 for that evidence and design
 * §5.1/§5.2 for the corrections it forced.
 *
 * <p>With no header there is nothing naming a column, so two things carry the whole contract, and
 * each has a test here that would fail if it were wrong:
 * <ul>
 *   <li><b>order</b> — columns bind by position, in model attribute declaration order, the one
 *       canonical order every path in {@code RosettaCsvMapper} uses. {@code User} is the type that
 *       distinguishes it: it declares {@code username} first but sorts alphabetically to
 *       {@code firstName} first, so a hand-authored file read directly tells the two apart. A
 *       write-then-read round trip cannot — it passes under either order, which is exactly how the
 *       alphabetical binding survived undetected before; and</li>
 *   <li><b>width</b> — the row must have exactly as many columns as the type has attributes. Jackson
 *       throws on a row that is too wide, but silently leaves the trailing attributes absent on one
 *       that is too narrow, so the mapper checks both.</li>
 * </ul>
 */
public class RosettaCsvMapperHeaderlessTest {

    private static final RosettaCSVConfiguration HEADERLESS =
            RosettaCSVConfiguration.builder().setHasHeader(false).build();

    private static RosettaCsvMapper headerlessMapper() {
        return (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(HEADERLESS).create();
    }

    private static RosettaCsvMapper mapperFor(RosettaCSVConfiguration config) {
        return (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
    }

    private static User buildUser() {
        return User.builder()
                .setUsername("username")
                .setIdentifier("identifier")
                .setFirstName("FirstName")
                .setLastName("LastName")
                .build();
    }

    // ---------------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------------

    /**
     * The characterisation of the old defect, inverted into the assertion that matters: a header-less
     * file no longer yields an all-null object. Every attribute of {@code User} is {@code (1..1)}, so
     * the four non-null assertions are the whole object.
     */
    @Test
    void aHeaderlessFileNoLongerReadsAsAnAllNullObject() throws JsonMappingException {
        User user = headerlessMapper().readValue("username,identifier,FirstName,LastName\n", User.class);

        assertNotNull(user.getUsername());
        assertNotNull(user.getIdentifier());
        assertNotNull(user.getFirstName());
        assertNotNull(user.getLastName());
        assertEquals(buildUser(), user);
    }

    /**
     * The test that proves declaration order rather than {@code schemaFor}'s alphabetical order.
     * {@code User} declares {@code username, identifier, firstName, lastName} and sorts to
     * {@code firstName, identifier, lastName, username}, so a file of {@code A,B,C,D} distinguishes
     * them: declaration order gives {@code username=A}, alphabetical order would give
     * {@code firstName=A} and {@code username=D}. Hand-authored and read directly — a round trip
     * through this mapper's own writer would pass under either order.
     */
    @Test
    void headerlessReadBindsByDeclarationOrderNotAlphabetically() throws JsonMappingException {
        User user = headerlessMapper().readValue("A,B,C,D\n", User.class);

        assertEquals(User.builder()
                .setUsername("A")
                .setIdentifier("B")
                .setFirstName("C")
                .setLastName("D")
                .build(), user);
    }

    /**
     * A single-row header-less file used to throw {@code MismatchedInputException} ("No content to
     * map due to end-of-input"): its one row was eaten as a header and nothing was left to bind. It
     * is the commonest shape of all — one CSV row per object — so it is pinned separately from the
     * multi-row case.
     */
    @Test
    void aSingleRowHeaderlessFileReads() throws JsonMappingException {
        User user = headerlessMapper().readValue("A,B,C,D\n", User.class);

        assertEquals("A", user.getUsername());
    }

    /**
     * {@code readValue} binds one object, so it takes the first row and ignores the rest — the same
     * contract the header-bearing path has ({@code RosettaCsvMapperTest} pins that one).
     */
    @Test
    void headerlessReadTakesTheFirstRowAndIgnoresLaterOnes() throws JsonMappingException {
        User user = headerlessMapper().readValue("A,B,C,D\nE,F,G,H\n", User.class);

        assertEquals("A", user.getUsername());
        assertEquals("D", user.getLastName());
    }

    // ---------------------------------------------------------------------------
    // Width
    // ---------------------------------------------------------------------------

    /**
     * A row narrower than the type is the dangerous half: jackson reports nothing and leaves the
     * trailing attributes absent, which for a mandatory attribute becomes a cardinality failure
     * somewhere later, far from the malformed file that caused it. There is no header to notice the
     * shortfall by, so the mapper measures the row itself.
     */
    @Test
    void aHeaderlessRowWithTooFewColumnsThrows() {
        RosettaCsvMapper mapper = headerlessMapper();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mapper.readValue("A,B,C\n", User.class));

        assertTrue(exception.getMessage().contains("3 column(s)"));
        assertTrue(exception.getMessage().contains("4 attribute(s)"));
        // The order is named, since "wrong width" is only actionable against the expected columns.
        assertTrue(exception.getMessage().contains("[username, identifier, firstName, lastName]"));
    }

    /**
     * Jackson throws on a row that is too wide by itself, but with a {@code CsvReadException} about
     * "too many entries" that names neither the type nor the expected columns. Checked here as well
     * so both directions of mismatch report the same thing.
     */
    @Test
    void aHeaderlessRowWithTooManyColumnsThrows() {
        RosettaCsvMapper mapper = headerlessMapper();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mapper.readValue("A,B,C,D,E\n", User.class));

        assertTrue(exception.getMessage().contains("5 column(s)"));
        assertTrue(exception.getMessage().contains("4 attribute(s)"));
    }

    /**
     * An empty document is left to jackson: it has no row to measure, and "no content to map" says
     * more than a column-count complaint about a file holding no columns.
     */
    @Test
    void anEmptyHeaderlessDocumentReportsNoContentRatherThanAWidthMismatch() {
        RosettaCsvMapper mapper = headerlessMapper();

        JsonMappingException exception = assertThrows(JsonMappingException.class,
                () -> mapper.readValue("", User.class));

        assertTrue(exception.getMessage().contains("end-of-input"));
    }

    /**
     * An optional attribute does not exempt a row from the width check. {@code NullableAttributes}
     * declares {@code id (1..1)} then {@code note (0..1)}; a one-column row is still refused, because
     * "the note column is missing" and "the note is empty" are different files and only the second is
     * writable by this mapper. An empty cell is how absence is spelled.
     */
    @Test
    void anOptionalTrailingAttributeStillNeedsItsColumn() throws JsonMappingException {
        RosettaCsvMapper mapper = headerlessMapper();

        assertThrows(IllegalStateException.class, () -> mapper.readValue("id1\n", NullableAttributes.class));

        NullableAttributes withEmptyCell = mapper.readValue("id1,\n", NullableAttributes.class);
        assertEquals("id1", withEmptyCell.getId());
        assertNull(withEmptyCell.getNote());
    }

    // ---------------------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------------------

    /**
     * {@code hasHeader} describes the file on both sides, so it suppresses the header row on write as
     * well as expecting none on read. Safe because there is one canonical column order across writer
     * and reader; the round-trip test below is the proof that the two sides share it.
     */
    @Test
    void headerlessWriteEmitsNoHeaderRow() throws JsonProcessingException {
        String csv = headerlessMapper().writeValueAsString(buildUser());

        assertEquals("username,identifier,FirstName,LastName\n", csv);
    }

    @Test
    void headerlessWriteThenReadRoundTrips() throws IOException {
        RosettaCsvMapper mapper = headerlessMapper();
        User original = buildUser();

        String csv = mapper.writeValueAsString(original);
        User roundTripped = mapper.readValue(csv, User.class);

        assertEquals(original, roundTripped);
    }

    /**
     * The dialect and {@code hasHeader} are independent: suppressing the header row does not disturb
     * the punctuation, and the columns stay in declaration order under a non-default delimiter.
     */
    @Test
    void headerlessWriteHonoursTheDialectAndRoundTrips() throws IOException {
        RosettaCSVConfiguration semicolon = RosettaCSVConfiguration.builder()
                .setHasHeader(false)
                .setDialect(CsvDialect.builder().setColumnDelimiter(';').build())
                .setListDelimiter("|")
                .build();
        RosettaCsvMapper mapper = mapperFor(semicolon);

        String csv = mapper.writeValueAsString(buildUser());

        assertEquals("username;identifier;FirstName;LastName\n", csv);
        assertEquals(buildUser(), mapper.readValue(csv, User.class));
    }

    // ---------------------------------------------------------------------------
    // Interaction with the other settings
    // ---------------------------------------------------------------------------

    /**
     * The list-element null-token pre-pass has to be told which columns it is looking at. With a
     * header row it reads them off the header; with none, the first row is data, so the mapper hands
     * over the declaration-order column names instead. Had it read the row, no cell would have
     * matched a multi-cardinality attribute name and nothing would have been stripped: {@code a;;b}
     * would have reached jackson as a three-element list containing an empty middle element, and
     * failed inside the generated immutable's null-rejecting list.
     */
    @Test
    void aHeaderlessListColumnStillHasItsNullTokensStripped() throws JsonMappingException {
        MultiCardinalityAttributes result =
                headerlessMapper().readValue("id1,EUR;;USD\n", MultiCardinalityAttributes.class);

        assertEquals("id1", result.getId());
        assertEquals(Arrays.asList("EUR", "USD"), result.getTags());
    }

    @Test
    void aHeaderlessListColumnRoundTrips() throws IOException {
        RosettaCsvMapper mapper = headerlessMapper();
        MultiCardinalityAttributes original = MultiCardinalityAttributes.builder()
                .setId("id1")
                .addTags(Arrays.asList("EUR", "USD", "GBP"))
                .build();

        String csv = mapper.writeValueAsString(original);
        assertEquals("id1,EUR;USD;GBP\n", csv);

        assertEquals(original, mapper.readValue(csv, MultiCardinalityAttributes.class));
    }

    /**
     * A non-empty {@code nullToken} still works with no header row — the pre-pass and the schema's own
     * null value both come from the configuration, neither from the header.
     */
    @Test
    void aHeaderlessReadHonoursANonEmptyNullToken() throws JsonMappingException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setHasHeader(false)
                .setNullToken("N/A")
                .build();

        NullableAttributes result = mapperFor(config).readValue("id1,N/A\n", NullableAttributes.class);

        assertEquals("id1", result.getId());
        assertNull(result.getNote());
    }

    /**
     * {@code readValue(URL)} hands a document straight to jackson when it can, and buffers it when
     * something needs the whole thing up front. A header-less read is one of those things — it has to
     * measure the first row — so this pins that the URL overload routes through the buffering path
     * rather than streaming past the width check with the wrong schema.
     */
    @Test
    void theUrlOverloadHonoursAHeaderlessConfiguration(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("headerless.csv");
        Files.write(file, "A,B,C,D\n".getBytes(StandardCharsets.UTF_8));

        User user = headerlessMapper().readValue(file.toUri().toURL(), User.class);

        assertEquals("A", user.getUsername());
        assertEquals("D", user.getLastName());
    }

    @Test
    void theUrlOverloadAppliesTheWidthCheck(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("narrow.csv");
        Files.write(file, "A,B,C\n".getBytes(StandardCharsets.UTF_8));

        RosettaCsvMapper mapper = headerlessMapper();

        assertThrows(IllegalStateException.class, () -> mapper.readValue(file.toUri().toURL(), User.class));
    }

    /**
     * A header-bearing read is untouched by any of this: the default configuration is unchanged, and
     * a file whose columns are in alphabetical rather than declaration order still binds by name.
     */
    @Test
    void aHeaderBearingReadIsUnaffected() throws JsonMappingException {
        User user = RosettaCsvMapper.createCsvObjectMapper().readValue(
                "firstName,identifier,lastName,username\nFirstName,identifier,LastName,username\n", User.class);

        assertEquals(buildUser(), user);
    }
}
