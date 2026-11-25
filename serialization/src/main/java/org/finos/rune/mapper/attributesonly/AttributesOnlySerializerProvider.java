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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.util.Objects;

/**
 * Writes each root value as standard Rune JSON into a buffer, then writes that JSON through
 * {@link AttributesOnlyStripper}. The standard serializers are not aware of the attributes-only setting.
 * Installed by {@link org.finos.rune.mapper.RuneJsonObjectMapper#attributesOnly()}; no other code constructs it.
 * <p>
 * Root writes from both {@code ObjectMapper} and {@code ObjectWriter} pass through here. So do the nested
 * {@code writeTree} calls a serializer makes while the root is being buffered; those are written as they are,
 * because stripping a fragment on its own would turn a reference-only value into {@code {}} instead of
 * omitting it.
 */
public class AttributesOnlySerializerProvider extends DefaultSerializerProvider {

    private static final long serialVersionUID = 1L;

    /** The attributes-only mapper this provider belongs to; the buffer's codec. */
    private final transient ObjectMapper owner;

    /**
     * Reads the buffer back with the mapper's own settings, so decimals keep their scale whatever generator
     * the caller supplied. It is {@link #owner}'s reader without {@code USE_BIG_DECIMAL_FOR_FLOATS}. The buffer keeps each number's own
     * type, and that feature would turn a {@code float} into the {@code BigDecimal} of its widened
     * {@code double} ({@code 0.1f} as {@code 0.10000000149011612}); a {@code BigDecimal} is read as one either way.
     */
    private final transient ObjectReader treeReader;

    public AttributesOnlySerializerProvider(ObjectMapper owner) {
        super();
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.treeReader = owner.reader().without(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    private AttributesOnlySerializerProvider(AttributesOnlySerializerProvider src) {
        super(src);
        this.owner = src.owner;
        this.treeReader = src.treeReader;
    }

    private AttributesOnlySerializerProvider(AttributesOnlySerializerProvider src, SerializationConfig config, SerializerFactory factory) {
        super(src, config, factory);
        this.owner = src.owner;
        this.treeReader = src.treeReader;
    }

    private AttributesOnlySerializerProvider(AttributesOnlySerializerProvider src, CacheProvider cacheProvider) {
        super(src, cacheProvider);
        this.owner = src.owner;
        this.treeReader = src.treeReader;
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
        writeStrippedIfRoot(gen, target -> super.serializeValue(target, value));
    }

    @Override
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType) throws IOException {
        writeStrippedIfRoot(gen, target -> super.serializeValue(target, value, rootType));
    }

    @Override
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType, JsonSerializer<Object> ser) throws IOException {
        writeStrippedIfRoot(gen, target -> super.serializeValue(target, value, rootType, ser));
    }

    @Override
    public void serializePolymorphic(JsonGenerator gen, Object value, JavaType rootType, JsonSerializer<Object> valueSer, TypeSerializer typeSer) throws IOException {
        writeStrippedIfRoot(gen, target -> super.serializePolymorphic(target, value, rootType, valueSer, typeSer));
    }

    /**
     * A nested call, made while a root is being buffered, writes straight to {@code gen}. A root call is
     * buffered and its stripped tree written directly, since root wrapping ({@code withRootName}) was
     * already applied while buffering.
     */
    private void writeStrippedIfRoot(JsonGenerator gen, ValueWriter valueWriter) throws IOException {
        if (gen instanceof RootBuffer) {
            valueWriter.writeTo(gen);
            return;
        }
        TokenBuffer buffer = new RootBuffer(owner);
        valueWriter.writeTo(buffer);
        JsonNode tree = treeReader.readTree(buffer.asParser());
        ((JsonSerializable) AttributesOnlyStripper.strip(tree)).serialize(gen, this);
    }

    /**
     * The buffer a root value is written into. Jackson builds a new provider for each call, so a nested
     * call is recognised by the generator it writes to, not by provider state.
     */
    private static final class RootBuffer extends TokenBuffer {
        RootBuffer(ObjectMapper codec) {
            super(codec, false);
        }
    }

    @FunctionalInterface
    private interface ValueWriter {
        void writeTo(JsonGenerator target) throws IOException;
    }
}
