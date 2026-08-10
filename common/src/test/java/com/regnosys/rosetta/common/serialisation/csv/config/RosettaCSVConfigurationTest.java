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

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

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
                () -> new RosettaCSVConfiguration(Optional.empty(), Optional.of(HeaderStyle.LABEL),
                        Optional.empty(), Optional.empty(), Optional.of(false)));

        assertTrue(exception.getMessage().contains("hasHeader"));
        assertTrue(exception.getMessage().contains("LABEL"));
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
    void noArgConstructorYieldsTheSameDefaultsAsAnEmptyDocument() throws IOException {
        assertEquals(RosettaCSVConfiguration.load(json("{}")), new RosettaCSVConfiguration());
    }
}
