package org.finos.rune.serialization;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJsonObjectMapper;

/**
 * A Jackson-based implementation of the {@link RuneJsonSerializer} interface
 * for serializing and deserializing Rune DSL objects to and from JSON.
 * <p>
 * This class uses a custom-configured {@link ObjectMapper} to handle JSON
 * serialization and deserialization. By default, it employs a {@link RuneJsonObjectMapper},
 * but a pre-configured {@code ObjectMapper} can also be injected via the constructor
 * to customize behavior.
 * </p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * // Default ObjectMapper
 * RuneJacksonJsonSerializer serializer = new RuneJacksonJsonSerializer();
 *
 * // Custom ObjectMapper
 * ObjectMapper customMapper = new ObjectMapper();
 * RuneJacksonJsonSerializer customSerializer = new RuneJacksonJsonSerializer(customMapper);
 * </pre>
 *
 * @see RuneJsonSerializer
 * @see com.fasterxml.jackson.databind.ObjectMapper
 * @see com.fasterxml.jackson.core.JsonProcessingException
 */
public class RuneJacksonJsonSerializer implements RuneJsonSerializer {

    private final ObjectMapper objectMapper;

    public RuneJacksonJsonSerializer() {
        objectMapper = new RuneJsonObjectMapper();
    }

    public RuneJacksonJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends RosettaModelObject> String toJson(T runeObject) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(runeObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type) {
        try {
            return objectMapper.readValue(runeJson, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
