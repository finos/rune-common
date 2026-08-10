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
