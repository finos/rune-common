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
import com.regnosys.rosetta.common.serialisation.RosettaCsvMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import csv.test.multi.MultiCardinalityAttributes;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Write-side tests for a multi-cardinality simple attribute serialising into a single delimited CSV
 * column (TASK-9540 session 1; see {@code csv-single-delimited-column-lists.md} §4.1). The read side
 * — deserialising such a column back into a list, and the round trip — is session 2.
 *
 * <p>{@code MultiCardinalityAttributes} ({@code csv.test.multi}) declares {@code id (1..1)} then
 * {@code tags (0..*)}, so column order alone would catch a delimiter wired to the wrong schema.
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
}
