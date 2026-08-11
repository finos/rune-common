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
import csv.test.nullable.NullableAttributes;
import csv.test.user.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@code nullTokens}: which cell values deserialise to an absent attribute, and which
 * token an absent attribute writes back as.
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

    @Test
    void emptyNullTokensListMeansNoCellDeserialisesToAbsent() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Collections.emptyList())
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        NullableAttributes result = mapper.readValue("id,note\nabc,\n", NullableAttributes.class);

        assertEquals("", result.getNote());
    }

    @Test
    void configuredNullTokensAllDeserialiseToAbsent() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Arrays.asList("", "NULL", "N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        assertNull(mapper.readValue("id,note\nabc,\n", NullableAttributes.class).getNote());
        assertNull(mapper.readValue("id,note\nabc,NULL\n", NullableAttributes.class).getNote());
        assertNull(mapper.readValue("id,note\nabc,N/A\n", NullableAttributes.class).getNote());
    }

    @Test
    void configuredNullTokensLeaveANonMatchingValueUntouched() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Arrays.asList("", "NULL", "N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        NullableAttributes result = mapper.readValue("id,note\nabc,a real value\n", NullableAttributes.class);

        assertEquals("a real value", result.getNote());
    }

    /**
     * A configuration naming more than one null token routes the document through a
     * read-substitute-rewrite pre-pass, and the pre-pass must not change the row's arity. It used
     * to: it substituted a Java {@code null}, and jackson's CSV generator omits a null element from
     * the column-less schema the pre-pass writes with, rather than emitting an empty field — so the
     * column disappeared and every column after it shifted one place left, binding each attribute
     * to its neighbour's value. Silent, and plausible rather than obviously wrong.
     *
     * <p>{@code User} is used rather than {@code NullableAttributes} because the shift is invisible
     * on a two-column type when it is the second column that is nulled: there is nothing after it to
     * shift. Four columns with the token in the middle is the smallest case that shows it.</p>
     */
    @Test
    void aNullTokenInAMiddleColumnDoesNotShiftTheColumnsAfterIt() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Arrays.asList("", "N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        User result = mapper.readValue(
                "username,identifier,firstName,lastName\nu1,N/A,f1,l1\n", User.class);

        assertEquals("u1", result.getUsername());
        assertNull(result.getIdentifier());
        assertEquals("f1", result.getFirstName());
        assertEquals("l1", result.getLastName());
    }

    /**
     * The same shift, from the other direction: this cell matches no <i>extra</i> token at all — it
     * is simply empty — but the pre-pass's raw reader maps a cell equal to the canonical token to
     * Java {@code null} via the schema's null value, so it was dropped on rewrite just the same.
     * Any absent cell outside the last column was affected, not only one spelt with an extra token.
     */
    @Test
    void anEmptyCellInAMiddleColumnDoesNotShiftTheColumnsAfterIt() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Arrays.asList("", "N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        User result = mapper.readValue(
                "username,identifier,firstName,lastName\nu1,,f1,l1\n", User.class);

        assertEquals("u1", result.getUsername());
        assertNull(result.getIdentifier());
        assertEquals("f1", result.getFirstName());
        assertEquals("l1", result.getLastName());
    }

    /**
     * The pre-pass re-renders the whole document, so a quoted cell elsewhere in the row has to
     * survive it byte-for-byte — including one holding the column delimiter, which is the case that
     * would break if the rewrite ever stopped going through the mapper's own dialect-aware writer.
     */
    @Test
    void aQuotedCellSurvivesTheNullTokenPrePass() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Arrays.asList("", "N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        User result = mapper.readValue(
                "username,identifier,firstName,lastName\nu1,N/A,\"First,Name\",l1\n", User.class);

        assertNull(result.getIdentifier());
        assertEquals("First,Name", result.getFirstName());
        assertEquals("l1", result.getLastName());
    }

    @Test
    void roundTripThroughAFileContainingAConfiguredNullTokenWritesBackTheCanonicalToken() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullTokens(Arrays.asList("", "NULL", "N/A"))
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();

        NullableAttributes read = mapper.readValue("id,note\nabc,N/A\n", NullableAttributes.class);
        assertNull(read.getNote());

        String written = mapper.writeValueAsString(read);

        // nullTokens.get(0) — "" — is the canonical token; the writer never echoes back the token
        // that happened to be read ("N/A").
        assertEquals("id,note\nabc,\n", written);
    }
}
