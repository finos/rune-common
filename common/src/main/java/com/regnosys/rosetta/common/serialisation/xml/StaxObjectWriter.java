package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.regnosys.rosetta.common.serialisation.xml.stax.RuneXmlWriter;

import java.io.IOException;

/**
 * Adapts {@link RuneXmlWriter} onto the {@link ObjectWriter} shape, so
 * {@code mapper.writerWithDefaultPrettyPrinter().withAttribute("schemaLocation", ...)
 * .writeValueAsString(obj)} keeps working unchanged against the StAX binder.
 *
 * <p>Package-private: only {@link StaxXmlObjectMapper} constructs these.
 */
class StaxObjectWriter extends ObjectWriter {

    private static final long serialVersionUID = 1L;

    private final transient ObjectMapper mapper;
    private final transient RuneXmlWriter delegate;

    StaxObjectWriter(ObjectMapper mapper, RuneXmlWriter delegate) {
        super(mapper, mapper.getSerializationConfig());
        this.mapper = mapper;
        this.delegate = delegate;
    }

    @Override
    public ObjectWriter withAttribute(Object key, Object value) {
        return new StaxObjectWriter(mapper, delegate.withAttribute(String.valueOf(key), String.valueOf(value)));
    }

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        try {
            return delegate.writeValueAsString(value);
        } catch (IOException e) {
            throw new JsonMappingException(e.getMessage(), e);
        }
    }
}
