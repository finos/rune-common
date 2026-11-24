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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Settings for a {@code RUNE_JSON} transform, read from the schema's {@code configPath}. They apply to the
 * whole document.
 * <p>
 * Not to be confused with {@link org.finos.rune.mapper.RuneJsonConfig}, which only names the Rune JSON meta
 * properties ({@code @type}, {@code @model}, {@code @version}).
 * <p>
 * {@code attributesOnly} (default {@code false}): write only model attributes, with no {@code @} properties.
 * See {@link org.finos.rune.mapper.RuneJsonObjectMapper#attributesOnly()}. Such JSON cannot be read back,
 * so the setting is for projections only.
 * <p>
 * Create one with {@link #load(InputStream)} or {@link #builder()}. An absent setting takes its default,
 * and {@link #EMPTY} is the configuration of a transform with no {@code configPath}.
 */
public class RuneJSONConfiguration {

    public static final RuneJSONConfiguration EMPTY = builder().build();

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
     * Loads a configuration from JSON. An unknown property is tolerated, not rejected.
     *
     * @throws IOException if the stream cannot be read or does not hold valid JSON
     */
    public static RuneJSONConfiguration load(InputStream input) throws IOException {
        ObjectMapper configurationMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        RuneJSONConfiguration configuration = configurationMapper.readValue(input, RuneJSONConfiguration.class);
        return configuration != null ? configuration : EMPTY;
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
