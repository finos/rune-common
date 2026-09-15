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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.regnosys.rosetta.common.serialisation.RosettaCsvMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import csv.test.metadata.MetadataAttributeHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Putting columns in declaration order must not change <em>which</em> columns there are.
 *
 * <p>The order is recovered from the generated {@code process} visitor, which does not report every property
 * jackson serialises — a metadata-annotated attribute arrives at {@code processRosetta} rather than
 * {@code processBasic}. Left out of the recovered order, such an attribute loses its column in the reordered
 * schema, and jackson then refuses to write the property at all ({@code Unrecognized column 'scheme'}) even
 * though nothing was assigned to it. {@code MetadataAttributeHolder} is the construct available here that
 * reproduces that.
 */
public class RosettaCsvMapperColumnSetTest {

    private static MetadataAttributeHolder holderWithoutScheme() {
        return MetadataAttributeHolder.builder()
                .setHead("h1")
                .setTail("t1")
                .build();
    }

    /**
     * An unassigned metadata attribute must still get its column and an empty cell. Without both halves of
     * the fix this throws {@code CsvWriteException: Unrecognized column 'scheme'}.
     */
    @Test
    void anAttributeTheVisitorDoesNotReportAsBasicStillGetsItsColumn() throws Exception {
        String csv = RosettaCsvMapper.createCsvObjectMapper().writeValueAsString(holderWithoutScheme());

        assertEquals("head,scheme,tail\nh1,,t1\n", csv);
    }

    /**
     * And it must keep its <em>declared</em> position, not be appended after the columns the visitor
     * did report. {@code scheme} is declared second, so it is written second — appending it instead
     * would give {@code head,tail,scheme}, which reads back with the last two values transposed on any
     * positional path.
     */
    @Test
    void suchAnAttributeKeepsItsDeclaredPosition() throws Exception {
        String csv = RosettaCsvMapper.createCsvObjectMapper().writeValueAsString(holderWithoutScheme());

        assertEquals("head,scheme,tail", csv.split("\\R")[0]);
    }

    /**
     * The header-less paths read the column count from the same reordered schema, so a narrowed set surfaces
     * there as a width complaint about a file that is in fact the right width. Asserted through a write/read
     * round trip, since both sides are built from the one helper.
     */
    @Test
    void aHeaderlessRoundTripAgreesOnTheColumnCount() throws Exception {
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(
                RosettaCSVConfiguration.builder().setHasHeader(false).build()).create();

        String csv = mapper.writeValueAsString(holderWithoutScheme());
        assertEquals("h1,,t1\n", csv, "three columns, the middle one empty");

        MetadataAttributeHolder roundTripped = mapper.readValue(csv, MetadataAttributeHolder.class);
        assertEquals(holderWithoutScheme(), roundTripped);
    }

    /**
     * The complement, so the above is not mistaken for support this mapper does not have: a
     * <em>populated</em> metadata attribute cannot be written to CSV, because jackson has no cell shape for
     * the {@code FieldWithMeta*} object it generates. Which is why the rune-dsl tabular rule is the right
     * place to refuse such a type.
     */
    @Test
    void aPopulatedMetadataAttributeStillCannotBeWritten() {
        MetadataAttributeHolder populated = MetadataAttributeHolder.builder()
                .setHead("h1")
                .setSchemeValue("S1")
                .setTail("t1")
                .build();

        Exception e = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> RosettaCsvMapper.createCsvObjectMapper().writeValueAsString(populated));

        org.junit.jupiter.api.Assertions.assertTrue(
                e.getMessage().contains("does not support Object values"),
                "expected jackson's nested-object refusal, got: " + e.getMessage());
    }

    /**
     * And the read side, for the same reason: a cell cannot be turned back into a
     * {@code FieldWithMeta*}. Pre-existing on every read path, and unchanged here.
     */
    @Test
    void aPopulatedMetadataColumnStillCannotBeRead() {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();

        JsonMappingException e = org.junit.jupiter.api.Assertions.assertThrows(JsonMappingException.class,
                () -> mapper.readValue("head,scheme,tail\nh1,S1,t1\n", MetadataAttributeHolder.class));

        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("FieldWithMetaString"),
                "expected the wrapper type to be named, got: " + e.getMessage());
    }
}
