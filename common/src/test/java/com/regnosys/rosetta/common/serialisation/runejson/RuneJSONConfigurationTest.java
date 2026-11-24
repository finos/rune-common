package com.regnosys.rosetta.common.serialisation.runejson;

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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuneJSONConfigurationTest {

    private static RuneJSONConfiguration load(String json) throws IOException {
        return RuneJSONConfiguration.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void loadsAttributesOnly() throws IOException {
        assertTrue(load("{\"attributesOnly\": true}").isAttributesOnly());
    }

    @Test
    void loadsAttributesOnlyFalse() throws IOException {
        assertFalse(load("{\"attributesOnly\": false}").isAttributesOnly());
    }

    @Test
    void defaultsAttributesOnlyToFalseWhenAbsent() throws IOException {
        assertFalse(load("{}").isAttributesOnly());
    }

    @Test
    void defaultsAttributesOnlyToFalseWhenNull() throws IOException {
        assertFalse(load("{\"attributesOnly\": null}").isAttributesOnly());
    }

    @Test
    void loadsNullDocumentAsEmpty() throws IOException {
        assertEquals(RuneJSONConfiguration.EMPTY, load("null"));
    }

    @Test
    void toleratesUnknownProperty() throws IOException {
        assertTrue(load("{\"attributesOnly\": true, \"somethingElse\": 1}").isAttributesOnly());
    }

    @Test
    void emptyIsNotAttributesOnly() {
        assertFalse(RuneJSONConfiguration.EMPTY.isAttributesOnly());
    }

    @Test
    void builderAndToBuilderCarryAttributesOnly() {
        RuneJSONConfiguration configuration = RuneJSONConfiguration.builder().setAttributesOnly(true).build();

        assertTrue(configuration.isAttributesOnly());
        assertEquals(configuration, configuration.toBuilder().build());
    }
}
