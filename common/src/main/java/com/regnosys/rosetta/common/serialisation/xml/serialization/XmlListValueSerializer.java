package com.regnosys.rosetta.common.serialisation.xml.serialization;

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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Writes a multi-cardinality attribute as an XSD list type: each item serialised by its own serializer,
 * then joined with single spaces into one value.
 */
public class XmlListValueSerializer extends StdSerializer<Collection<?>> implements ContextualSerializer {

    private static final long serialVersionUID = 1L;

    private final JsonSerializer<Object> itemSerializer;

    public XmlListValueSerializer() {
        this(null);
    }

    private XmlListValueSerializer(JsonSerializer<Object> itemSerializer) {
        super(Collection.class, false);
        this.itemSerializer = itemSerializer;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        JavaType type = property == null ? null : property.getType();
        if (type == null || !type.isCollectionLikeType()) {
            return prov.reportBadDefinition(type,
                    "xmlList is configured on " + describe(property) + ", which is not a multi-cardinality attribute");
        }
        return new XmlListValueSerializer(prov.findValueSerializer(type.getContentType(), property));
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Collection<?> value) {
        return value == null || value.isEmpty();
    }

    @Override
    public void serialize(Collection<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        List<String> items = new ArrayList<>(value.size());
        for (Object item : value) {
            if (item != null) {
                items.add(serializeItem(item, gen, provider));
            }
        }
        gen.writeString(String.join(" ", items));
    }

    private String serializeItem(Object item, JsonGenerator gen, SerializerProvider provider) throws IOException {
        TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false);
        itemSerializer.serialize(item, buffer, provider);
        try (JsonParser parser = buffer.asParser()) {
            JsonToken token = parser.nextToken();
            if (token == null || !token.isScalarValue()) {
                throw JsonMappingException.from(gen,
                        "xmlList items must serialise to a single value, but " + item.getClass().getName() + " does not");
            }
            return parser.getText();
        }
    }

    private static String describe(BeanProperty property) {
        return property == null ? "an unnamed property" : "'" + property.getName() + "'";
    }
}
