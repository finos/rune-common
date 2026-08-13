package com.regnosys.rosetta.common.serialisation.csv.config;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RosettaCSVConfigurationTest {

    private static InputStream json(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void emptyDocumentLoadsToRfc4180Defaults() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.load(json("{}"));

        assertEquals(RosettaCSVConfiguration.EMPTY, config);
        assertTrue(config.isHasHeader());
        assertEquals(HeaderStyle.ATTRIBUTE_NAME, config.getHeaderStyle());
        assertEquals("", config.getNullToken());
        assertEquals(',', config.getDialect().getColumnDelimiter());
        assertEquals('"', config.getDialect().getQuoteChar());
        // null, not the quote character: RFC 4180 has no escape character, only the doubled quote.
        assertNull(config.getDialect().getEscapeChar());
    }

    @Test
    void everyFieldRoundTripsThroughLoad() throws IOException {
        String document = "{"
                + "\"dialect\": {\"columnDelimiter\": \";\", \"quoteChar\": \"'\", \"escapeChar\": \"\\\\\"},"
                + "\"headerStyle\": \"LABEL\","
                + "\"listDelimiter\": \"|\","
                + "\"nullToken\": \"N/A\","
                + "\"hasHeader\": true"
                + "}";

        RosettaCSVConfiguration config = RosettaCSVConfiguration.load(json(document));

        assertEquals(';', config.getDialect().getColumnDelimiter());
        assertEquals('\'', config.getDialect().getQuoteChar());
        assertEquals(Character.valueOf('\\'), config.getDialect().getEscapeChar());
        assertEquals(HeaderStyle.LABEL, config.getHeaderStyle());
        assertEquals("|", config.getListDelimiter());
        assertEquals("N/A", config.getNullToken());
        assertTrue(config.isHasHeader());
    }

    @Test
    void unknownPropertyIsToleratedNotThrown() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.load(json("{\"notAProperty\": 42}"));

        assertEquals(RosettaCSVConfiguration.EMPTY, config);
    }

    /**
     * {@code hasHeader=false} is accepted: reads bind by position in declaration order, writes suppress the
     * header row.
     */
    @Test
    void hasHeaderFalseIsAccepted() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder().setHasHeader(false).build();

        assertFalse(config.isHasHeader());
        assertEquals(HeaderStyle.ATTRIBUTE_NAME, config.getHeaderStyle());
    }

    /**
     * A label is header text; a file declared to have no header row has nowhere to carry one. The two
     * settings contradict each other and neither half can be assumed to be the one the caller meant.
     */
    @Test
    void hasHeaderFalseWithLabelHeaderStyleThrowsAtConstruction() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RosettaCSVConfiguration.builder()
                        .setHasHeader(false)
                        .setHeaderStyle(HeaderStyle.LABEL)
                        .build());

        assertTrue(exception.getMessage().contains("headerStyle=LABEL"));
        assertTrue(exception.getMessage().contains("hasHeader=false"));
    }

    /**
     * If the list delimiter and the column delimiter were the same character, a list element and a column
     * boundary inside the same cell could not be told apart. Rejected at construction, where every other
     * invalid combination in this class is rejected.
     */
    @Test
    void listDelimiterEqualToColumnDelimiterThrowsAtConstruction() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RosettaCSVConfiguration.builder()
                        .setDialect(CsvDialect.builder().setColumnDelimiter(';').build())
                        .setListDelimiter(";")
                        .build());

        assertTrue(exception.getMessage().contains("listDelimiter"));
        assertTrue(exception.getMessage().contains("columnDelimiter"));
    }

    @Test
    void defaultListDelimiterDoesNotCollideWithADistinctColumnDelimiter() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setDialect(CsvDialect.builder().setColumnDelimiter('|').build())
                .build();

        assertEquals(";", config.getListDelimiter());
        assertEquals('|', config.getDialect().getColumnDelimiter());
    }

    @Test
    void aHeaderlessConfigurationLoadsFromJson() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.load(json("{\"hasHeader\": false}"));

        assertFalse(config.isHasHeader());
        assertEquals(RosettaCSVConfiguration.EMPTY.toBuilder().setHasHeader(false).build(), config);
    }

    @Test
    void loadingAHeaderlessLabelledConfigurationFailsWithTheSameCause() {
        // Jackson wraps the constructor's exception; the rejection still happens at construction,
        // load() just cannot surface it unwrapped.
        ValueInstantiationException exception = assertThrows(ValueInstantiationException.class,
                () -> RosettaCSVConfiguration.load(json("{\"hasHeader\": false, \"headerStyle\": \"LABEL\"}")));

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void anUncustomisedBuilderYieldsTheSameDefaultsAsAnEmptyDocument() throws IOException {
        assertEquals(RosettaCSVConfiguration.load(json("{}")), RosettaCSVConfiguration.builder().build());
        assertEquals(RosettaCSVConfiguration.EMPTY, RosettaCSVConfiguration.builder().build());
    }

    @Test
    void aSettingLeftUnsetOnTheBuilderTakesItsDefault() {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setListDelimiter("|")
                .build();

        assertEquals("|", config.getListDelimiter());
        assertEquals(CsvDialect.RFC_4180, config.getDialect());
        assertEquals(HeaderStyle.ATTRIBUTE_NAME, config.getHeaderStyle());
        assertEquals("", config.getNullToken());
        assertTrue(config.isHasHeader());
    }

    @Test
    void anUncustomisedDialectBuilderYieldsRfc4180() {
        assertEquals(CsvDialect.RFC_4180, CsvDialect.builder().build());
        assertEquals(';', CsvDialect.builder().setColumnDelimiter(';').build().getColumnDelimiter());
        // The other two settings are untouched by setting the delimiter.
        assertEquals('"', CsvDialect.builder().setColumnDelimiter(';').build().getQuoteChar());
        assertNull(CsvDialect.builder().setColumnDelimiter(';').build().getEscapeChar());
    }

    /**
     * The escape character is independent of the quote character. Defaulted to the quote character instead
     * of {@code null}, customising only {@code quoteChar} would leave it holding the <i>default</i> quote
     * character, and the mapper would configure a real escape character the caller never asked for.
     */
    @Test
    void customisingTheQuoteCharDoesNotIntroduceAnEscapeChar() {
        CsvDialect dialect = CsvDialect.builder().setQuoteChar('\'').build();

        assertEquals('\'', dialect.getQuoteChar());
        assertNull(dialect.getEscapeChar());
    }

    @Test
    void anEscapeCharCanStillBeSetExplicitly() {
        assertEquals(Character.valueOf('\\'), CsvDialect.builder().setEscapeChar('\\').build().getEscapeChar());
        CsvDialect escaped = CsvDialect.builder().setEscapeChar('\\').build();
        assertNull(escaped.toBuilder().setEscapeChar(null).build().getEscapeChar());
    }

    @Test
    void toBuilderVariesOneSettingAndKeepsTheRest() throws IOException {
        RosettaCSVConfiguration loaded = RosettaCSVConfiguration.load(
                json("{\"listDelimiter\": \"|\", \"nullToken\": \"NULL\"}"));

        RosettaCSVConfiguration varied = loaded.toBuilder().setHeaderStyle(HeaderStyle.LABEL).build();

        assertEquals(loaded, varied.toBuilder().setHeaderStyle(HeaderStyle.ATTRIBUTE_NAME).build());
        assertEquals("|", varied.getListDelimiter());
        assertEquals("NULL", varied.getNullToken());
        assertEquals(HeaderStyle.LABEL, varied.getHeaderStyle());
    }

    /**
     * The constructor takes plain nullable types rather than {@code Optional}s, so — unlike
     * {@code RosettaXMLConfiguration} — this class deserialises through a caller's own mapper with no
     * {@code Jdk8Module} registered.
     */
    @Test
    void deserialisesThroughAnUnconfiguredObjectMapper() throws IOException {
        RosettaCSVConfiguration config = new ObjectMapper()
                .readValue("{\"listDelimiter\": \"|\"}", RosettaCSVConfiguration.class);

        assertEquals("|", config.getListDelimiter());
        assertEquals(CsvDialect.RFC_4180, config.getDialect());
        assertTrue(config.isHasHeader());
    }
}
