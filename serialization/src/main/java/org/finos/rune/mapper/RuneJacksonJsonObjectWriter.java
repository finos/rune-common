package org.finos.rune.mapper;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneDataType;

import java.util.Arrays;

/**
 * A custom {@link ObjectWriter} implementation for the Rune DSL.
 * <p>
 * This writer extends Jackson's {@link ObjectWriter} to provide JSON serialization
 * tailored to the Rune DSL. It ensures that serialized objects include metadata
 * at the top level and maintain a structure that is easy to read and closely aligned
 * with the DSL's format.
 * </p>
 *
 * <h2>Usage:</h2>
 * This writer is used internally by the {@link RuneJacksonJsonObjectMapper} and
 * is not typically instantiated directly. Instead, it is accessed through the
 * configured {@link ObjectMapper}.
 * <pre>
 * RuneJacksonJsonObjectMapper objectMapper = new RuneJacksonJsonObjectMapper();
 * String json = objectMapper.writeValueAsString(runeObject);
 * </pre>
 *
 * @see ObjectWriter
 * @see RuneJacksonJsonObjectMapper
 */
public class RuneJacksonJsonObjectWriter extends ObjectWriter {
    private final ObjectMapper mapper;


    protected RuneJacksonJsonObjectWriter(ObjectMapper mapper, SerializationConfig config, FormatSchema s) {
        super(mapper, config, s);
        this.mapper = mapper;
    }

    public RuneJacksonJsonObjectWriter(ObjectMapper mapper, SerializationConfig config) {
        super(mapper, config);
        this.mapper = mapper;
    }

    protected RuneJacksonJsonObjectWriter(ObjectMapper mapper, SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
        super(mapper, config, rootType, pp);
        this.mapper = mapper;
    }

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        if (value instanceof RosettaModelObject) {
            RosettaModelObject rosettaModelObject = (RosettaModelObject) value;
            ObjectNode objectNode = addTopLevelMeta(rosettaModelObject);
            return super.writeValueAsString(objectNode);
        }

        return super.writeValueAsString(value);
    }

    private <T extends RosettaModelObject> ObjectNode addTopLevelMeta(T runeObject) {
        Class<? extends RosettaModelObject> runeType = runeObject.getType();
        return Arrays.stream(runeType.getAnnotations())
                .filter(allAnnotations -> allAnnotations.annotationType().equals(RuneDataType.class)).findFirst().map(a -> {
                    RuneDataType runeDataType = (RuneDataType) a;
                    ObjectNode modifiedNode = mapper.createObjectNode();
                    modifiedNode = modifiedNode.put("@model", runeDataType.model())
                            .put("@type", runeType.getCanonicalName())
                            .put("@version", runeDataType.version());

                    ObjectNode originalNode = mapper.valueToTree(runeObject);
                    modifiedNode.setAll(originalNode);
                    return modifiedNode;

                }).orElse(mapper.valueToTree(runeObject));
    }
}
