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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.rosetta.model.lib.RosettaModelObject;

import java.io.IOException;

/**
 * Refuses to read a model object. Attributes-only Rune JSON has no {@code @type} to resolve choice options
 * and subtypes, and no {@code @key}/{@code @ref} to restore references, so reading it would build the
 * wrong objects. Other reads, such as a JSON tree, still work.
 * <p>
 * Root reads from both {@code ObjectMapper} and {@code ObjectReader} pass through here.
 */
public class WriteOnlyDeserializationContext extends DefaultDeserializationContext {

    private static final long serialVersionUID = 1L;

    public WriteOnlyDeserializationContext() {
        super(BeanDeserializerFactory.instance, null);
    }

    private WriteOnlyDeserializationContext(WriteOnlyDeserializationContext src) {
        super(src);
    }

    private WriteOnlyDeserializationContext(WriteOnlyDeserializationContext src, DeserializerFactory factory) {
        super(src, factory);
    }

    private WriteOnlyDeserializationContext(WriteOnlyDeserializationContext src, DeserializationConfig config,
                                            JsonParser parser, InjectableValues values) {
        super(src, config, parser, values);
    }

    private WriteOnlyDeserializationContext(WriteOnlyDeserializationContext src, DeserializationConfig config) {
        super(src, config);
    }

    private WriteOnlyDeserializationContext(WriteOnlyDeserializationContext src, CacheProvider cacheProvider) {
        super(src, cacheProvider);
    }

    @Override
    public DefaultDeserializationContext copy() {
        return new WriteOnlyDeserializationContext(this);
    }

    @Override
    public DefaultDeserializationContext with(DeserializerFactory factory) {
        return new WriteOnlyDeserializationContext(this, factory);
    }

    @Override
    public DefaultDeserializationContext withCaches(CacheProvider cacheProvider) {
        return new WriteOnlyDeserializationContext(this, cacheProvider);
    }

    @Override
    public DefaultDeserializationContext createInstance(DeserializationConfig config, JsonParser parser, InjectableValues values) {
        return new WriteOnlyDeserializationContext(this, config, parser, values);
    }

    @Override
    public DefaultDeserializationContext createDummyInstance(DeserializationConfig config) {
        return new WriteOnlyDeserializationContext(this, config);
    }

    @Override
    public Object readRootValue(JsonParser parser, JavaType valueType, JsonDeserializer<Object> deserializer, Object valueToUpdate) throws IOException {
        if (containsModelType(valueType)) {
            throw InvalidDefinitionException.from(parser,
                    "Cannot read " + valueType.toCanonical() + ": this RuneJsonObjectMapper writes "
                            + "attributes-only Rune JSON, which has no @type or reference metadata to read a model "
                            + "object back from. Read Rune JSON with a RuneJsonObjectMapper that is not attributes-only.",
                    valueType);
        }
        return super.readRootValue(parser, valueType, deserializer, valueToUpdate);
    }

    /**
     * Whether the type is a model type or holds one, such as {@code List<Trade>}.
     */
    private static boolean containsModelType(JavaType type) {
        if (RosettaModelObject.class.isAssignableFrom(type.getRawClass())) {
            return true;
        }
        for (int i = 0; i < type.containedTypeCount(); i++) {
            if (containsModelType(type.containedType(i))) {
                return true;
            }
        }
        return type.isArrayType() && containsModelType(type.getContentType());
    }
}
