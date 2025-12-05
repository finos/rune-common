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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneDataType;
import org.finos.rune.mapper.processor.SerializationPreProcessor;

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
 * This writer is used internally by the {@link RuneJsonObjectMapper} and
 * is not typically instantiated directly. Instead, it is accessed through the
 * configured {@link ObjectMapper}.
 * <pre>
 * RuneJacksonJsonObjectMapper objectMapper = new RuneJacksonJsonObjectMapper();
 * String json = objectMapper.writeValueAsString(runeObject);
 * </pre>
 *
 * @see ObjectWriter
 * @see RuneJsonObjectMapper
 */
public class RuneJsonObjectWriter extends ObjectWriter {
    private final ObjectMapper mapper;
    private final SerializationPreProcessor serializationPreProcessor;

    protected RuneJsonObjectWriter(ObjectMapper mapper, SerializationConfig config, FormatSchema s) {
        super(mapper, config, s);
        this.mapper = mapper;
        serializationPreProcessor = new SerializationPreProcessor();
    }

    public RuneJsonObjectWriter(ObjectMapper mapper, SerializationConfig config) {
        super(mapper, config);
        this.mapper = mapper;
        serializationPreProcessor = new SerializationPreProcessor();
    }

    protected RuneJsonObjectWriter(ObjectMapper mapper, SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
        super(mapper, config, rootType, pp);
        this.mapper = mapper;
        serializationPreProcessor = new SerializationPreProcessor();
    }

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        if (value instanceof RosettaModelObject) {
            RosettaModelObject processed = serializationPreProcessor.process((RosettaModelObject) value);
            return super.writeValueAsString(createTopLevelHeadersWrapper(processed));
        }

        return super.writeValueAsString(value);
    }

    private Object createTopLevelHeadersWrapper(RosettaModelObject rosettaModelObject) {
        Class<? extends RosettaModelObject> runeType = rosettaModelObject.getType();
        return Arrays.stream(runeType.getAnnotations())
                .filter(allAnnotations -> allAnnotations.annotationType().equals(RuneDataType.class))
                .findFirst()
                .<Object>map(a -> {
                    RuneDataType runeDataType = (RuneDataType) a;
                    TopLevel topLevel = new TopLevel();
                    topLevel.setModel(runeDataType.model());
                    topLevel.setType(runeType.getCanonicalName());
                    topLevel.setVersion(runeDataType.version());
                    topLevel.setRosettaModelObject(rosettaModelObject);
                    return topLevel;
                }).orElse(rosettaModelObject);
    }

    private static class TopLevel {
        private String model;
        private String type;
        private String version;
        private RosettaModelObject rosettaModelObject;

        @JsonGetter(RuneJsonConfig.MetaProperties.MODEL)
        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        @JsonGetter(RuneJsonConfig.MetaProperties.TYPE)
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @JsonGetter(RuneJsonConfig.MetaProperties.VERSION)
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @JsonGetter("rosettaModelObject")
        @JsonUnwrapped
        public RosettaModelObject getRosettaModelObject() {
            return rosettaModelObject;
        }

        public void setRosettaModelObject(RosettaModelObject rosettaModelObject) {
            this.rosettaModelObject = rosettaModelObject;
        }
    }
}
