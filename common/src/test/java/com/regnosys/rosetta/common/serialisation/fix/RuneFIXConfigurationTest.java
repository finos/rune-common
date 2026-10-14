package com.regnosys.rosetta.common.serialisation.fix;

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

import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.RosettaModelObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuneFIXConfigurationTest {

    private static final String TEST_DICTIONARY_PATH = "FIX50SP2.xml";

    private static InputStream json(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Deserialization & Default Validation
     */

    @Test
    void emptyConfigurationThrowsException() {
        assertThrows(
                IOException.class,
                () -> RuneFIXConfiguration.load(json("{}")));
    }

    @Test
    void builderWithoutDictionaryPathThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RuneFIXConfiguration.builder().build());
    }

    @Test
    void unknownPropertyIsToleratedNotThrown() throws IOException {
        RuneFIXConfiguration config = RuneFIXConfiguration.load(
                json("{"
                        + "\"dictionaryPath\": \"" + TEST_DICTIONARY_PATH + "\","
                        + "\"notAProperty\": 42"
                        + "}"));

        assertEquals(TEST_DICTIONARY_PATH, config.getDictionaryPath());
        assertTrue(config.getMessageTypeRouting().isEmpty());
    }

    @Test
    void deserializesThroughAnUnconfiguredObjectMapper() throws IOException {
        RuneFIXConfiguration config = new ObjectMapper()
                .readValue(
                        "{"
                                + "\"dictionaryPath\": \"" + TEST_DICTIONARY_PATH + "\","
                                + "\"messageTypeRouting\": {\"TradeCaptureReport_RTS1_RTS2\": \"AE\"}"
                                + "}",
                        RuneFIXConfiguration.class);

        assertEquals(TEST_DICTIONARY_PATH, config.getDictionaryPath());
        assertEquals(
                "AE",
                config.getMessageTypeRouting().get("TradeCaptureReport_RTS1_RTS2"));
    }

    /**
     * Routing Logic & Validation
     */

    @Test
    void getMsgTypeForResolvesKnownClass() {
        RuneFIXConfiguration config = RuneFIXConfiguration.builder()
                .setDictionaryPath(TEST_DICTIONARY_PATH)
                .setMessageTypeRouting(ImmutableMap.of("KnownClassStub", "XX"))
                .build();

        assertEquals("XX", config.getMsgTypeFor(KnownClassStub.class));
    }

    @Test
    void getMsgTypeForWithoutConfiguredMappingThrowsException() {
        RuneFIXConfiguration config = RuneFIXConfiguration.builder()
                .setDictionaryPath(TEST_DICTIONARY_PATH)
                .build();

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> config.getMsgTypeFor(UnknownClassStub.class)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> config.getMsgTypeFor(null))
        );
    }

    /**
     * Round-Trip & Immutability
     */

    @Test
    void everyFieldRoundTripsThroughLoad() throws IOException {
        String document = "{"
                + "\"dictionaryPath\": \"mifir/FIX50SP2_MarketAxess.xml\","
                + "\"messageTypeRouting\": {\"TradeCaptureReport_MiFIR_RTS1_RTS2\": \"AE\", \"ExecutionReport_FIX\": \"8\"}"
                + "}";

        RuneFIXConfiguration config = RuneFIXConfiguration.load(json(document));

        assertEquals("mifir/FIX50SP2_MarketAxess.xml", config.getDictionaryPath());
        assertEquals(2, config.getMessageTypeRouting().size());
        assertEquals("AE", config.getMessageTypeRouting().get("TradeCaptureReport_MiFIR_RTS1_RTS2"));
        assertEquals("8", config.getMessageTypeRouting().get("ExecutionReport_FIX"));
    }

    @Test
    void toBuilderVariesOneSettingAndKeepsTheRest() throws IOException {
        RuneFIXConfiguration loaded = RuneFIXConfiguration.load(
                json("{\"dictionaryPath\": \"custom.xml\"}"));

        RuneFIXConfiguration varied = loaded.toBuilder().build();

        assertEquals("custom.xml", varied.getDictionaryPath());
    }

    /**
     * Dummy classes for simulating Rune domain models in routing tests
     */

    private abstract static class KnownClassStub implements RosettaModelObject {}
    private abstract static class UnknownClassStub implements RosettaModelObject {}
}
