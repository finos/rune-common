package com.regnosys.rosetta.common.serialisation.runejson.config;

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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Settings for a {@code RUNE_JSON} transform, read from the schema's {@code configPath}. They apply to the
 * whole document.
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
    private static final ObjectMapper CONFIGURATION_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private final boolean attributesOnly;

    @JsonCreator
    private RuneJSONConfiguration(@JsonProperty("attributesOnly") boolean attributesOnly) {
        this.attributesOnly = attributesOnly;
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
        try (JsonParser parser = CONFIGURATION_MAPPER.createParser(input)) {
            JsonToken first = parser.nextToken();
            if (first == null || first == JsonToken.VALUE_NULL) {
                return EMPTY;
            }
            UnknownSettingsCollector unknownSettings = new UnknownSettingsCollector();
            RuneJSONConfiguration configuration = CONFIGURATION_MAPPER.readerFor(RuneJSONConfiguration.class)
                    .withHandler(unknownSettings)
                    .readValue(parser);
            unknownSettings.warnIfAny(source);
            return configuration;
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

    private static class UnknownSettingsCollector extends DeserializationProblemHandler {
        private final List<String> unknown = new ArrayList<>();
        private Collection<Object> known;

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer,
                                             Object beanOrClass, String propertyName) throws IOException {
            unknown.add(propertyName);
            known = deserializer.getKnownPropertyNames();
            p.skipChildren();
            return true;
        }

        void warnIfAny(String source) {
            if (!unknown.isEmpty()) {
                LOGGER.warn("{} has unknown settings {}, which are ignored. Known settings: {}.", source, unknown, known);
            }
        }
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
