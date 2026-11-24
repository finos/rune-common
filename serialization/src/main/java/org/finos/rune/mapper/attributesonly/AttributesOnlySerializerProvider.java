package org.finos.rune.mapper.attributesonly;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;

/**
 * Writes each root value as standard Rune JSON into a buffer, then writes that JSON through
 * {@link AttributesOnlyStripper}. The standard serializers are not aware of the attributes-only setting.
 * <p>
 * Root writes from both {@code ObjectMapper} and {@code ObjectWriter} pass through here.
 */
public class AttributesOnlySerializerProvider extends DefaultSerializerProvider {

    private static final long serialVersionUID = 1L;

    public AttributesOnlySerializerProvider() {
        super();
    }

    private AttributesOnlySerializerProvider(AttributesOnlySerializerProvider src) {
        super(src);
    }

    private AttributesOnlySerializerProvider(AttributesOnlySerializerProvider src, SerializationConfig config, SerializerFactory factory) {
        super(src, config, factory);
    }

    private AttributesOnlySerializerProvider(AttributesOnlySerializerProvider src, CacheProvider cacheProvider) {
        super(src, cacheProvider);
    }

    @Override
    public DefaultSerializerProvider copy() {
        return new AttributesOnlySerializerProvider(this);
    }

    @Override
    public DefaultSerializerProvider createInstance(SerializationConfig config, SerializerFactory factory) {
        return new AttributesOnlySerializerProvider(this, config, factory);
    }

    @Override
    public DefaultSerializerProvider withCaches(CacheProvider cacheProvider) {
        return new AttributesOnlySerializerProvider(this, cacheProvider);
    }

    @Override
    public void serializeValue(JsonGenerator gen, Object value) throws IOException {
        TokenBuffer buffer = newBuffer(gen);
        super.serializeValue(buffer, value);
        writeStripped(gen, buffer);
    }

    @Override
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType) throws IOException {
        TokenBuffer buffer = newBuffer(gen);
        super.serializeValue(buffer, value, rootType);
        writeStripped(gen, buffer);
    }

    @Override
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType, JsonSerializer<Object> ser) throws IOException {
        TokenBuffer buffer = newBuffer(gen);
        super.serializeValue(buffer, value, rootType, ser);
        writeStripped(gen, buffer);
    }

    @Override
    public void serializePolymorphic(JsonGenerator gen, Object value, JavaType rootType, JsonSerializer<Object> valueSer, TypeSerializer typeSer) throws IOException {
        TokenBuffer buffer = newBuffer(gen);
        super.serializePolymorphic(buffer, value, rootType, valueSer, typeSer);
        writeStripped(gen, buffer);
    }

    private static TokenBuffer newBuffer(JsonGenerator gen) {
        return new TokenBuffer(requireCodec(gen), false);
    }

    private void writeStripped(JsonGenerator gen, TokenBuffer buffer) throws IOException {
        JsonNode tree = requireCodec(gen).readTree(buffer.asParser());
        // A root null is buffered as a null token, which readTree reports as no content.
        if (tree == null) {
            super.serializeValue(gen, null);
            return;
        }
        super.serializeValue(gen, AttributesOnlyStripper.strip(tree));
    }

    /**
     * The codec reads the buffer back with the mapper's own settings, so decimals keep their scale.
     */
    private static ObjectCodec requireCodec(JsonGenerator gen) {
        ObjectCodec codec = gen.getCodec();
        if (codec == null) {
            throw new IllegalStateException("Attributes-only Rune JSON needs a JsonGenerator created by its "
                    + "RuneJsonObjectMapper; this generator has no ObjectCodec.");
        }
        return codec;
    }
}
