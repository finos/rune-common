package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a multi-cardinality attribute from an XSD list type: one value split on whitespace, each
 * token read by the item type's own deserializer.
 */
public class XmlListValueDeserializer extends StdDeserializer<List<Object>> implements ContextualDeserializer {

    private static final long serialVersionUID = 1L;

    private final String xmlName;
    private final JsonDeserializer<Object> itemDeserializer;

    public XmlListValueDeserializer() {
        super(List.class);
        this.xmlName = null;
        this.itemDeserializer = null;
    }

    private XmlListValueDeserializer(JavaType type, String xmlName, JsonDeserializer<Object> itemDeserializer) {
        super(type);
        this.xmlName = xmlName;
        this.itemDeserializer = itemDeserializer;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType type = property == null ? null : property.getType();
        if (type == null || !type.isCollectionLikeType()) {
            String name = property == null ? "an unnamed property" : "'" + property.getName() + "'";
            return ctxt.reportBadDefinition(type,
                    "xmlList is configured on " + name + ", which is not a multi-cardinality attribute");
        }
        return new XmlListValueDeserializer(type, property.getName(),
                ctxt.findContextualValueDeserializer(type.getContentType(), property));
    }

    @Override
    public List<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text;
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            text = p.getText();
        } else if (p.hasToken(JsonToken.START_OBJECT)) {
            text = readElementText(p, ctxt);
        } else {
            return castToList(ctxt.handleUnexpectedToken(getValueType(ctxt), p));
        }
        List<Object> items = new ArrayList<>();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return items;
        }
        for (String token : trimmed.split("\\s+")) {
            items.add(deserializeItem(token, p, ctxt));
        }
        return items;
    }

    /**
     * Jackson XML wraps an element bound to a collection in a virtual wrapper of the same name, so its
     * text arrives as an object holding one field: the element's own name, or the empty name for text.
     */
    private String readElementText(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = null;
        while (p.nextToken() == JsonToken.FIELD_NAME) {
            String name = p.currentName();
            p.nextToken();
            if (!(name.isEmpty() || name.equals(xmlName)) || !p.hasToken(JsonToken.VALUE_STRING)) {
                return ctxt.reportInputMismatch(this,
                        "An xmlList element '%s' holds text only, but found '%s' (%s)", xmlName, name, p.currentToken());
            }
            if (text != null) {
                return ctxt.reportInputMismatch(this,
                        "An xmlList element '%s' appears more than once; its items belong in one element", xmlName);
            }
            text = p.getText();
        }
        return text == null ? "" : text;
    }

    private Object deserializeItem(String token, JsonParser p, DeserializationContext ctxt) throws IOException {
        TokenBuffer buffer = new TokenBuffer(p.getCodec(), false);
        buffer.writeString(token);
        try (JsonParser itemParser = buffer.asParser(p.getCodec())) {
            itemParser.nextToken();
            return itemDeserializer.deserialize(itemParser, ctxt);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToList(Object value) {
        return (List<Object>) value;
    }
}
