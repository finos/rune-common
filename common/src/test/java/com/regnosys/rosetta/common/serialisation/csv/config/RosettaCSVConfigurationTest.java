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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(Arrays.asList(""), config.getNullTokens());
        assertEquals(',', config.getDialect().getColumnDelimiter());
        assertEquals('"', config.getDialect().getQuoteChar());
        assertEquals('"', config.getDialect().getEscapeChar());
    }

    @Test
    void everyFieldRoundTripsThroughLoad() throws IOException {
        String document = "{"
                + "\"dialect\": {\"columnDelimiter\": \";\", \"quoteChar\": \"'\", \"escapeChar\": \"\\\\\"},"
                + "\"headerStyle\": \"LABEL\","
                + "\"listDelimiter\": \"|\","
                + "\"nullTokens\": [\"\", \"NULL\", \"N/A\"],"
                + "\"hasHeader\": true"
                + "}";

        RosettaCSVConfiguration config = RosettaCSVConfiguration.load(json(document));

        assertEquals(';', config.getDialect().getColumnDelimiter());
        assertEquals('\'', config.getDialect().getQuoteChar());
        assertEquals('\\', config.getDialect().getEscapeChar());
        assertEquals(HeaderStyle.LABEL, config.getHeaderStyle());
        assertEquals("|", config.getListDelimiter());
        assertEquals(Arrays.asList("", "NULL", "N/A"), config.getNullTokens());
        assertTrue(config.isHasHeader());
    }

    @Test
    void unknownPropertyIsToleratedNotThrown() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.load(json("{\"notAProperty\": 42}"));

        assertEquals(RosettaCSVConfiguration.EMPTY, config);
    }

    @Test
    void headerlessLabelHeaderStyleThrowsAtConstruction() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RosettaCSVConfiguration.builder()
                        .setHeaderStyle(HeaderStyle.LABEL)
                        .setHasHeader(false)
                        .build());

        assertTrue(exception.getMessage().contains("hasHeader"));
        assertTrue(exception.getMessage().contains("LABEL"));
    }

    /**
     * If the list delimiter and the column delimiter were the same character, a list element and a
     * column boundary inside the same cell could not be told apart. Design §3.7 lists this as a
     * configure-time validation on the import side; rejecting it here too closes the runtime case at
     * the same place every other invalid combination in this class is rejected — construction.
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
    void loadingSuchAConfigurationFailsWithTheSameCause() {
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
        assertEquals(Arrays.asList(""), config.getNullTokens());
        assertTrue(config.isHasHeader());
    }

    @Test
    void anUncustomisedDialectBuilderYieldsRfc4180() {
        assertEquals(CsvDialect.RFC_4180, CsvDialect.builder().build());
        assertEquals(';', CsvDialect.builder().setColumnDelimiter(';').build().getColumnDelimiter());
        // The other two settings are untouched by setting the delimiter.
        assertEquals('"', CsvDialect.builder().setColumnDelimiter(';').build().getQuoteChar());
        assertEquals('"', CsvDialect.builder().setColumnDelimiter(';').build().getEscapeChar());
    }

    @Test
    void toBuilderVariesOneSettingAndKeepsTheRest() throws IOException {
        RosettaCSVConfiguration loaded = RosettaCSVConfiguration.load(
                json("{\"listDelimiter\": \"|\", \"nullTokens\": [\"\", \"NULL\"]}"));

        RosettaCSVConfiguration varied = loaded.toBuilder().setHasHeader(false).build();

        assertEquals(loaded, varied.toBuilder().setHasHeader(true).build());
        assertEquals("|", varied.getListDelimiter());
        assertEquals(Arrays.asList("", "NULL"), varied.getNullTokens());
        assertEquals(false, varied.isHasHeader());
    }

    /**
     * The constructor takes plain nullable types rather than {@code Optional}s, so — unlike
     * {@code RosettaXMLConfiguration} — this class deserialises through a caller's own mapper with no
     * {@code Jdk8Module} registered. Before that change an absent property bound to a null
     * {@code Optional} and the constructor threw {@code NullPointerException}.
     */
    @Test
    void deserialisesThroughAnUnconfiguredObjectMapper() throws IOException {
        RosettaCSVConfiguration config = new ObjectMapper()
                .readValue("{\"listDelimiter\": \"|\"}", RosettaCSVConfiguration.class);

        assertEquals("|", config.getListDelimiter());
        assertEquals(CsvDialect.RFC_4180, config.getDialect());
        assertTrue(config.isHasHeader());
    }

    @Test
    void nullTokensAreCopiedDefensivelyAndReturnedUnmodifiable() {
        List<String> supplied = new ArrayList<>(Arrays.asList("", "NULL"));
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder().setNullTokens(supplied).build();

        supplied.add("N/A");

        assertEquals(Arrays.asList("", "NULL"), config.getNullTokens());
        assertThrows(UnsupportedOperationException.class, () -> config.getNullTokens().add("N/A"));
    }
}
