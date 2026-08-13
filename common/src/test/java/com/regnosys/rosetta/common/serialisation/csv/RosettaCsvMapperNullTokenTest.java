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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@code nullToken}: the cell value that deserialises to an absent attribute, and that an
 * absent attribute writes back as.
 *
 * <p>Uses the generated {@code NullableAttributes} type ({@code id (1..1)}, {@code note (0..1)}) —
 * neither existing test type ({@code User}, {@code LabelledTrade}) declares an optional attribute,
 * and one is needed to observe absent-vs-empty-string at all.</p>
 */
public class RosettaCsvMapperNullTokenTest {

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
     * A configured null token in a middle column routes the document through the list-stripping pre-pass
     * ({@code stripListElementsMeaningAbsent}), which must not change the row's arity. A Java {@code null}
     * left in the row would be omitted by jackson's CSV generator rather than written as an empty field,
     * dropping the column and shifting every later one a place left — silent, and plausible rather than
     * obviously wrong.
     *
     * <p>{@code MultiCardinalityAttributes} rather than a scalar-only type, since a scalar-only type never
     * enters the pre-pass at all: it returns its input unchanged whenever the type has no multi-cardinality
     * attribute, before reading a single row.</p>
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
     * The same arity guarantee from the other direction: this cell matches no <i>configured</i> token — it
     * is simply empty — but the pre-pass's raw reader maps a cell equal to the schema's null value to Java
     * {@code null} just the same, so any absent cell outside the last column is at risk, not only one spelt
     * with a non-default token.
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
     * The list-stripping pre-pass re-renders the whole document once it strips anything, so a quoted cell
     * elsewhere in the row has to survive byte-for-byte — including one holding the column delimiter, which
     * is what breaks if the rewrite stops going through the mapper's own dialect-aware writer.
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
     * A cell read as absent writes back as the same token it was read as.
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
     * The write side: an absent optional attribute writes the <i>configured</i> token, not a blank cell. It
     * has to reach the generator as a null for the schema's null value to be consulted at all, which is why
     * the CSV path takes {@code ALWAYS} serialisation inclusion — see
     * {@code RosettaObjectMapperCreator.serializationInclusion}. A blank cell here would be a file this
     * mapper's own reader misreads as the empty string.
     */
    @Test
    void anAbsentAttributeWritesTheConfiguredNullToken() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes noNote = NullableAttributes.builder().setId("abc").build();

        String written = mapper.writeValueAsString(noNote);

        assertEquals("id,note\nabc,N/A\n", written);
    }

    /**
     * What this mapper writes, this mapper reads back as the same value — an absent attribute must not come
     * back as the empty string.
     */
    @Test
    void anAbsentAttributeRoundTripsUnderANonEmptyNullToken() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes original = NullableAttributes.builder().setId("abc").build();

        String written = mapper.writeValueAsString(original);
        NullableAttributes read = mapper.readValue(written, NullableAttributes.class);

        assertNull(read.getNote());
        assertEquals(original, read);
    }

    /**
     * The empty-string escape hatch, in <b>both</b> directions. {@code nullToken}'s javadoc tells a
     * deployment needing to carry the empty string as data to configure a token that cannot occur in its
     * feed; that advice only holds if a genuine {@code ""} writes as a blank cell and reads back as
     * {@code ""} rather than being confused with absence either way.
     */
    @Test
    void aGenuineEmptyStringRoundTripsUnderANonEmptyNullToken() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes original = NullableAttributes.builder().setId("abc").setNote("").build();

        String written = mapper.writeValueAsString(original);

        assertEquals("id,note\nabc,\n", written);
        assertEquals("", mapper.readValue(written, NullableAttributes.class).getNote());
        assertEquals(original, mapper.readValue(written, NullableAttributes.class));
    }

    /**
     * The whole-cell counterpart of {@code listElementEqualToAConfiguredNullTokenIsRejectedAtWriteTime}: the
     * schema's null value turns a cell equal to the token back into an absent attribute, so writing that
     * string as a value loses it silently. Refused where the bad value is known rather than emitted into a
     * file this mapper's own reader reads as absent.
     */
    @Test
    void aValueEqualToAConfiguredNullTokenIsRejectedAtWriteTime() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes value = NullableAttributes.builder().setId("abc").setNote("N/A").build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("note"));
        assertTrue(exception.getMessage().contains("N/A"));
        assertTrue(exception.getMessage().contains("null token"));
    }

    /**
     * The same on a {@code (1..1)} attribute, where the loss is a cardinality violation rather than merely a
     * different string.
     */
    @Test
    void aMandatoryValueEqualToAConfiguredNullTokenIsRejectedAtWriteTime() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes value = NullableAttributes.builder().setId("N/A").setNote("a real value").build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains("id"));
        assertTrue(exception.getMessage().contains("N/A"));
    }

    /**
     * What has <i>not</i> changed. Under the default empty token an empty cell is CSV's only spelling of
     * absence, so an empty-string value has no other form to take — it is unrepresentable rather than
     * misconfigured, and refusing it would fail writes that succeed today. It still writes, and still comes
     * back absent; {@code aGenuineEmptyStringRoundTripsUnderANonEmptyNullToken} is the configuration that
     * carries it as data.
     */
    @Test
    void anEmptyStringIsStillWritableUnderTheDefaultNullToken() throws IOException {
        RosettaCsvMapper mapper = RosettaCsvMapper.createCsvObjectMapper();
        NullableAttributes original = NullableAttributes.builder().setId("abc").setNote("").build();

        String written = mapper.writeValueAsString(original);

        assertEquals("id,note\nabc,\n", written);
        assertNull(mapper.readValue(written, NullableAttributes.class).getNote());
    }

    /**
     * A non-empty token constrains only the exact token, not any cell that merely contains it.
     */
    @Test
    void aValueMerelyContainingTheNullTokenIsWritable() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setNullToken("N/A")
                .build();
        RosettaCsvMapper mapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        NullableAttributes original = NullableAttributes.builder().setId("abc").setNote("XN/AY").build();

        String written = mapper.writeValueAsString(original);

        assertEquals("id,note\nabc,XN/AY\n", written);
        assertEquals(original, mapper.readValue(written, NullableAttributes.class));
    }
}
