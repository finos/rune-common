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
import org.finos.rune.mapper.RuneJacksonObjectMapper;

public class RuneJacksonJsonSerializer implements RuneJsonSerializer {

    private final RuneJacksonObjectMapper objectMapper;

    public RuneJacksonJsonSerializer() {
        objectMapper = new RuneJacksonObjectMapper();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
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
