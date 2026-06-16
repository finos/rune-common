package com.regnosys.rosetta.common.serialisation.csv;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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
import com.regnosys.rosetta.common.serialisation.RosettaCsvMapper;
import com.rosetta.model.lib.functions.LabelProvider;
import csv.test.user.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link RosettaCsvMapper} label-header behaviour (tests #1–5 from the plan).
 *
 * <p>Uses the existing generated {@code User} type (fields: firstName, identifier, lastName,
 * username — alphabetical CSV column order) together with hand-written stub
 * {@link LabelProvider} implementations, following the same pattern as
 * {@link LabelProviderResolverTest}.
 */
public class RosettaCsvMapperLabelledTest {

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static User buildUser() {
        return User.builder()
                .setFirstName("Alice")
                .setIdentifier("id-001")
                .setLastName("Smith")
                .setUsername("asmith")
                .build();
    }

    /** LabelProvider backed by a fixed map; returns null for unmapped paths. */
    private static LabelProvider mapProvider(Map<String, String> labels) {
        return path -> labels.get(path.buildPath());
    }

    // ---------------------------------------------------------------------------
    // Test #1 — Labels present → label headers
    // ---------------------------------------------------------------------------

    /**
     * When every column has a label the CSV header must use the label text, not the
     * attribute name.  Values must appear in the same (alphabetical) column order.
     */
    @Test
    void shouldUseLabelsAsHeadersWhenAllColumnsAreLabelled() throws JsonProcessingException {
        Map<String, String> labels1 = new HashMap<>();
        labels1.put("firstName",  "First Name");
        labels1.put("identifier", "ID");
        labels1.put("lastName",   "Last Name");
        labels1.put("username",   "User Name");
        LabelProvider provider = mapProvider(labels1);

        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);
        String csv = mapper.writeValueAsString(buildUser());

        String expected = "\"First Name\",ID,\"Last Name\",\"User Name\"\n"
                + "Alice,id-001,Smith,asmith\n";
        assertEquals(expected, csv);
    }

    // ---------------------------------------------------------------------------
    // Test #2 — Label absent → attribute-name header (per-column fallback)
    // ---------------------------------------------------------------------------

    /**
     * When the provider returns null for every path the header must fall back to the
     * attribute name for each column.
     */
    @Test
    void shouldFallBackToAttributeNamesWhenNoLabelsAreProvided() throws JsonProcessingException {
        // Provider that always returns null — simulates a function whose type has no [label] annotations
        LabelProvider provider = path -> null;

        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);
        String csv = mapper.writeValueAsString(buildUser());

        String expected = "firstName,identifier,lastName,username\n"
                + "Alice,id-001,Smith,asmith\n";
        assertEquals(expected, csv);
    }

    // ---------------------------------------------------------------------------
    // Test #3 — Mixed labelled/unlabelled attributes
    // ---------------------------------------------------------------------------

    /**
     * When only some columns have labels the header must use the label for labelled
     * columns and the attribute name for unlabelled ones.
     */
    @Test
    void shouldMixLabelsAndAttributeNamesWhenOnlySomeColumnsAreLabelled() throws JsonProcessingException {
        // Only firstName and lastName are labelled; identifier and username are not
        Map<String, String> labels3 = new HashMap<>();
        labels3.put("firstName", "First Name");
        labels3.put("lastName",  "Last Name");
        LabelProvider provider = mapProvider(labels3);

        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);
        String csv = mapper.writeValueAsString(buildUser());

        String expected = "\"First Name\",identifier,\"Last Name\",username\n"
                + "Alice,id-001,Smith,asmith\n";
        assertEquals(expected, csv);
    }

    // ---------------------------------------------------------------------------
    // Test #4 — Plain CSV unchanged (regression guard)
    // ---------------------------------------------------------------------------

    /**
     * The no-arg {@code createCsvObjectMapper()} (plain CSV, no provider) must produce
     * output byte-for-byte identical to the original behaviour — attribute-name headers.
     * This is the regression guard: the {@code CSV_LABELLED} path must not affect plain CSV.
     */
    @Test
    void shouldProduceAttributeNameHeadersWhenNoProviderIsSupplied() throws JsonProcessingException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        String csv = mapper.writeValueAsString(buildUser());

        String expected = "firstName,identifier,lastName,username\n"
                + "Alice,id-001,Smith,asmith\n";
        assertEquals(expected, csv);
    }

    // ---------------------------------------------------------------------------
    // Test #5 — Duplicate labels (mirror csv012/csv016 from BnmCsvIRSType)
    // ---------------------------------------------------------------------------

    /**
     * When two distinct columns share the same label text both columns must emit that
     * label in the header, and each column's value must remain in its own position.
     * Mirrors the {@code csv012_Pay_Rate__OP_1_CP__Num} / {@code csv012_Pay_Rate__OP_1_CP_}
     * duplicate in {@code BnmCsvIRSType}.
     *
     * <p>The {@code User} type has four distinct columns; we map two of them
     * ({@code firstName} and {@code lastName}) to the same label text to exercise the
     * duplicate-label path.
     */
    @Test
    void shouldEmitDuplicateHeaderTextWhenTwoColumnsShareTheSameLabel() throws JsonProcessingException {
        // firstName and lastName both get the same label — distinct columns, same header text
        Map<String, String> labels5 = new HashMap<>();
        labels5.put("firstName",  "Name");
        labels5.put("identifier", "ID");
        labels5.put("lastName",   "Name");   // duplicate label
        labels5.put("username",   "User Name");
        LabelProvider provider = mapProvider(labels5);

        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper(provider);
        String csv = mapper.writeValueAsString(buildUser());

        // Columns are in alphabetical order: firstName, identifier, lastName, username
        // firstName → "Name", identifier → "ID", lastName → "Name", username → "User Name"
        String expected = "Name,ID,Name,\"User Name\"\n"
                + "Alice,id-001,Smith,asmith\n";
        assertEquals(expected, csv);
    }
}
