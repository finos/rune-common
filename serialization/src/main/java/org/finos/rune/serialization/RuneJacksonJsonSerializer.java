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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneDataType;
import org.finos.rune.mapper.RuneJacksonObjectMapper;

import java.util.Arrays;

public class RuneJacksonJsonSerializer implements RuneJsonSerializer {

    private final ObjectMapper objectMapper;

    public RuneJacksonJsonSerializer() {
        objectMapper = new RuneJacksonObjectMapper();
    }

    public RuneJacksonJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends RosettaModelObject> String toJson(T runeObject) {
        try {
            ObjectNode objectNode = addTopLevelMeta(runeObject);
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends RosettaModelObject> ObjectNode addTopLevelMeta(T runeObject) {
        Class<? extends RosettaModelObject> runeType = runeObject.getType();
        return Arrays.stream(runeType.getAnnotations())
                .filter(allAnnotations -> allAnnotations.annotationType().equals(RuneDataType.class)).findFirst().map(a -> {
                    RuneDataType runeDataType = (RuneDataType) a;
                    ObjectNode modifiedNode = objectMapper.valueToTree(runeObject);

                    return modifiedNode.put("@model", runeDataType.model())
                            .put("@type", runeType.getCanonicalName())
                            .put("@version", runeDataType.version());

                }).orElse(objectMapper.valueToTree(runeObject));
    }

    private static ObjectNode addField(ObjectNode objectNode, String fieldName, String fieldValue) {
        return objectNode.put(fieldName, fieldValue);
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
