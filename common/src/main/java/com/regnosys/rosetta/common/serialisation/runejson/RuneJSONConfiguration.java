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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Settings for a {@code RUNE_JSON} transform, read from the schema's {@code configPath}. They apply to the
 * whole document.
 * <p>
 * Not to be confused with {@link org.finos.rune.mapper.RuneJsonConfig}, which only names the Rune JSON meta
 * properties.
 * <p>
 * {@code attributesOnly} (default {@code false}): write only model attributes, with no {@code @} properties.
 * See {@link org.finos.rune.mapper.RuneJsonObjectMapper#attributesOnly()}. Such JSON cannot be read back,
 * so the setting is for projections only.
 * <p>
 * Create one with {@link #load(InputStream)} or {@link #builder()}. An absent setting takes its default, an
 * unknown one is ignored with a warning, and {@link #EMPTY} is the configuration with every setting at its
 * default.
 */
public class RuneJSONConfiguration {

    public static final RuneJSONConfiguration EMPTY = builder().build();

    private static final Logger LOGGER = LoggerFactory.getLogger(RuneJSONConfiguration.class);
    private static final List<String> KNOWN_SETTINGS = Collections.singletonList("attributesOnly");
    private static final ObjectMapper CONFIGURATION_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private final boolean attributesOnly;

    @JsonCreator
    private RuneJSONConfiguration(@JsonProperty("attributesOnly") Boolean attributesOnly) {
        this.attributesOnly = attributesOnly != null && attributesOnly;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().setAttributesOnly(attributesOnly);
    }

    /**
     * Loads a configuration from JSON. An unknown property is ignored, with a warning.
     *
     * @throws IOException if the stream cannot be read or does not hold a valid configuration
     */
    public static RuneJSONConfiguration load(InputStream input) throws IOException {
        return load(input, "Rune JSON configuration");
    }

    /**
     * {@link #load(InputStream)}, naming {@code source} in the warning for an unknown property.
     */
    public static RuneJSONConfiguration load(InputStream input, String source) throws IOException {
        JsonNode document = CONFIGURATION_MAPPER.readTree(input);
        if (document == null || document.isNull() || document.isMissingNode()) {
            return EMPTY;
        }
        warnOfUnknownSettings(document, source);
        return CONFIGURATION_MAPPER.treeToValue(document, RuneJSONConfiguration.class);
    }

    private static void warnOfUnknownSettings(JsonNode document, String source) {
        if (!document.isObject()) {
            return;
        }
        List<String> unknown = new ArrayList<>();
        document.fieldNames().forEachRemaining(name -> {
            if (!KNOWN_SETTINGS.contains(name)) {
                unknown.add(name);
            }
        });
        if (!unknown.isEmpty()) {
            LOGGER.warn("{} has unknown settings {}, which are ignored. Known settings: {}.", source, unknown, KNOWN_SETTINGS);
        }
    }

    public boolean isAttributesOnly() {
        return attributesOnly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributesOnly);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return attributesOnly == ((RuneJSONConfiguration) obj).attributesOnly;
    }

    @Override
    public String toString() {
        return "RuneJSONConfiguration{attributesOnly=" + attributesOnly + '}';
    }

    public static class Builder {
        private boolean attributesOnly;

        private Builder() {
        }

        public Builder setAttributesOnly(boolean attributesOnly) {
            this.attributesOnly = attributesOnly;
            return this;
        }

        public RuneJSONConfiguration build() {
            return new RuneJSONConfiguration(attributesOnly);
        }
    }
}
