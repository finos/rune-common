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
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.stax.RuneXmlMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Adapts {@link RuneXmlMapper} (the Jackson-free StAX binder) onto the {@link ObjectMapper}
 * shape that existing {@code RosettaObjectMapperCreator.forXML(...)} callers already depend on:
 * {@code readValue(...)}, {@code writeValueAsString(...)}, and
 * {@code writerWithDefaultPrettyPrinter().withAttribute(...).writeValueAsString(...)}.
 *
 * <p>Only those entry points are overridden. Every other inherited {@link ObjectMapper} method
 * still operates on Jackson's own (otherwise-unused) default configuration and is not expected
 * to be called against this class.
 */
public class StaxXmlObjectMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    private final transient RuneXmlMapper delegate;

    public StaxXmlObjectMapper(RosettaXMLConfiguration config, ClassLoader classLoader) {
        this.delegate = new RuneXmlMapper(config, classLoader);
    }

    @Override
    public <T> T readValue(String content, Class<T> valueType) throws JsonMappingException {
        try {
            return delegate.readValue(content, valueType);
        } catch (IOException e) {
            throw new JsonMappingException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T readValue(Reader src, Class<T> valueType) throws IOException {
        return delegate.readValue(src, valueType);
    }

    @Override
    public <T> T readValue(InputStream src, Class<T> valueType) throws IOException {
        return delegate.readValue(src, valueType);
    }

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        try {
            return delegate.writeValueAsString(value);
        } catch (IOException e) {
            throw new JsonMappingException(e.getMessage(), e);
        }
    }

    @Override
    public ObjectWriter writer() {
        return new StaxObjectWriter(this, delegate.writer());
    }

    @Override
    public ObjectWriter writerWithDefaultPrettyPrinter() {
        return new StaxObjectWriter(this, delegate.writerWithDefaultPrettyPrinter());
    }
}
