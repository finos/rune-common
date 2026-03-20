package org.finos.rune.mapper.choice;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Custom deserializer for choice types that handles flattened choice representation.
 *
 * When deserializing a choice type, this deserializer reads the @type and @data (for primitives)
 * or @type and fields (for data types), and constructs the appropriate choice wrapper.
 */
public class ChoiceTypeDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {

    private final JavaType choiceType;

    public ChoiceTypeDeserializer() {
        super(Object.class);
        this.choiceType = null;
    }

    public ChoiceTypeDeserializer(JavaType choiceType) {
        super(choiceType);
        this.choiceType = choiceType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType type = property != null ? property.getType() : choiceType;

        // Only use this deserializer for choice types (interfaces with "Choice" in name)
        if (type != null && type.isInterface() && type.getRawClass().getSimpleName().contains("Choice")) {
            return new ChoiceTypeDeserializer(type);
        }

        // For non-choice types, return null to use default deserializer
        return null;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (p.currentToken() != JsonToken.START_OBJECT) {
            ctxt.reportInputMismatch(this, "Expected START_OBJECT token for choice type");
            return null;
        }

        ObjectNode node = p.readValueAsTree();

        // Get the @type field
        String typeStr = node.has(RuneJsonConfig.MetaProperties.TYPE) ?
            node.get(RuneJsonConfig.MetaProperties.TYPE).asText() : null;

        if (typeStr == null) {
            ctxt.reportInputMismatch(this, "Missing @type field in choice type");
            return null;
        }

        // Check if this is a primitive type
        if (isPrimitiveType(typeStr)) {
            // Get the @data field
            if (!node.has(RuneJsonConfig.MetaProperties.DATA)) {
                ctxt.reportInputMismatch(this, "Missing @data field for primitive choice type");
                return null;
            }

            Object primitiveValue = deserializePrimitive(typeStr, node.get(RuneJsonConfig.MetaProperties.DATA).asText(), ctxt);
            return wrapInChoiceType(primitiveValue, ctxt);
        } else {
            // It's a data type - deserialize it
            try {
                Class<?> dataTypeClass = ctxt.findClass(typeStr);

                // Remove @type field before deserializing the data type
                node.remove(RuneJsonConfig.MetaProperties.TYPE);

                JsonParser newParser = node.traverse(p.getCodec());
                newParser.nextToken();

                Object dataValue = ctxt.readValue(newParser, dataTypeClass);
                return wrapInChoiceType(dataValue, ctxt);
            } catch (ClassNotFoundException e) {
                ctxt.reportInputMismatch(this, "Could not find class: " + typeStr);
                return null;
            }
        }
    }

    private boolean isPrimitiveType(String typeStr) {
        return "string".equals(typeStr) ||
               "int".equals(typeStr) ||
               "long".equals(typeStr) ||
               "number".equals(typeStr) ||
               "boolean".equals(typeStr);
    }

    private Object deserializePrimitive(String typeStr, String value, DeserializationContext ctxt) throws IOException {
        switch (typeStr) {
            case "string":
                return value;
            case "int":
                return Integer.parseInt(value);
            case "long":
                return Long.parseLong(value);
            case "number":
                return new BigDecimal(value);
            case "boolean":
                return Boolean.parseBoolean(value);
            default:
                ctxt.reportInputMismatch(this, "Unknown primitive type: " + typeStr);
                return null;
        }
    }

    private Object wrapInChoiceType(Object value, DeserializationContext ctxt) throws IOException {
        // For choice types, we just return the unwrapped value.
        // The builder will handle wrapping it appropriately.
        return value;
    }
}
