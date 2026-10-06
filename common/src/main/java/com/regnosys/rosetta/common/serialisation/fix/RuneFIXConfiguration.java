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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.RosettaModelObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;


/**
 * Global settings for FIX application-dictionary resolution and model-to-MsgType routing.
 *
 * <p>{@code dictionaryPath} is mandatory and identifies the application
 * {@link quickfix.DataDictionary} XML resource used by the integration. This
 * configuration does not provide a default dictionary or FIX session settings.</p>
 *
 * <p>The configuration contains no per-field tag mapping. Property-to-tag
 * binding is provided by the model labels, repeating-group metadata is resolved
 * from the QuickFIX/J {@link quickfix.DataDictionary}, and scalar formatting is
 * determined by the source value and FIX field type.</p>
 *
 * <p>{@code messageTypeRouting} maps serialized Rune model classes to their FIX
 * MsgType values. Each model serialized by the mapper must have an explicit
 * non-blank routing entry.</p>
 *
 * <p><b>Construction.</b> A configuration can be created by either:</p>
 * <ul>
 *   <li>{@link #load(InputStream)}, using a JSON configuration document; or</li>
 *   <li>{@link #builder()}, using the Java builder API, with
 *       {@link #toBuilder()} available for creating a modified copy.</li>
 * </ul>
 */
public class RuneFIXConfiguration {

    private final String dictionaryPath;
    private final Map<String, String> messageTypeRouting;

    /**
     * Not public: construct through {@link #builder()} or {@link #load(InputStream)}.
     *
     * @param dictionaryPath        required classpath location of the application DataDictionary XML file
     * @param messageTypeRouting    mappings from model class names to FIX MsgType values
     *
     * @throws IllegalArgumentException if {@code dictionaryPath} is missing or blank
     */
    @JsonCreator
    private RuneFIXConfiguration(
            @JsonProperty("dictionaryPath") String dictionaryPath,
            @JsonProperty("messageTypeRouting") Map<String, String> messageTypeRouting) {

        if (dictionaryPath == null || dictionaryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("dictionaryPath is required and must identify an application DataDictionary");
        } else {
            this.dictionaryPath = dictionaryPath;
        }
        this.messageTypeRouting = messageTypeRouting == null
                ? Collections.emptyMap()
                : ImmutableMap.copyOf(messageTypeRouting);
    }

    /**
     * A builder with every setting unset.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder pre-populated with this configuration's values.
     */
    public Builder toBuilder() {
        return builder()
                .setDictionaryPath(dictionaryPath)
                .setMessageTypeRouting(messageTypeRouting);
    }

    /**
     * Loads a configuration from JSON. An unknown property is tolerated, not rejected.
     *
     * @param input the JSON document
     * @return the validated configuration described by the document
     * @throws IOException if the stream cannot be read or does not hold valid JSON
     */
    public static RuneFIXConfiguration load(InputStream input) throws IOException {
        ObjectMapper fixConfigurationMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        return fixConfigurationMapper.readValue(input, RuneFIXConfiguration.class);
    }

    public String getDictionaryPath() {
        return dictionaryPath;
    }

    public Map<String, String> getMessageTypeRouting() {
        return messageTypeRouting;
    }

    /**
     * Resolves the configured FIX MsgType Tag for a Rune model class.
     *
     * <p>The model class simple name is used as the key in
     * {@code messageTypeRouting}. Every serializable model must have an explicit
     * non-blank mapping; no default MsgType is assumed.</p>
     *
     * @param valueType the Rune model class being serialized
     * @return the configured FIX MsgType value
     * @throws IllegalArgumentException if {@code valueType} is {@code null}, or if
     *         no non-blank MsgType mapping exists for the model class
     */
    public String getMsgTypeFor(Class<? extends RosettaModelObject> valueType) {
        if (valueType == null) {
            throw new IllegalArgumentException("Value type is required to resolve FIX MsgType.");
        }

        String msgType = messageTypeRouting.get(valueType.getSimpleName());
        if (msgType == null || msgType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "No FIX MsgType configured for model type " + valueType.getName());
        }

        return msgType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dictionaryPath, messageTypeRouting);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RuneFIXConfiguration other = (RuneFIXConfiguration) obj;
        return Objects.equals(dictionaryPath, other.dictionaryPath)
                && Objects.equals(messageTypeRouting, other.messageTypeRouting);
    }

    @Override
    public String toString() {
        return "RuneFIXConfiguration{dictionaryPath='" + dictionaryPath + '\''
                + ", messageTypeRouting=" + messageTypeRouting + '}';
    }

    /**
     * Builds a {@link RuneFIXConfiguration}.
     *
     * @throws IllegalArgumentException if dictionaryPath is missing or blank
     */
    public static class Builder {
        private String dictionaryPath;
        private Map<String, String> messageTypeRouting;

        private Builder() {}

        /**
         * Sets the classpath location of the application DataDictionary XML file.
         *
         * @param dictionaryPath required application DataDictionary path
         * @return this builder
         */
        public Builder setDictionaryPath(String dictionaryPath) {
            this.dictionaryPath = dictionaryPath;
            return this;
        }

        /**
         * Sets mappings from model class names to FIX MsgType values.
         *
         * @param messageTypeRouting model-to-FIX MsgType mappings
         * @return this builder
         */
        public Builder setMessageTypeRouting(Map<String, String> messageTypeRouting) {
            this.messageTypeRouting = messageTypeRouting;
            return this;
        }

        /**
         * Builds a configuration.
         *
         * @throws IllegalArgumentException if dictionaryPath is missing or blank
         */
        public RuneFIXConfiguration build() {
            return new RuneFIXConfiguration(dictionaryPath, messageTypeRouting);
        }
    }
}
