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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;

/**
 * Custom TypeDeserializer for choice types that handles flattened representation.
 *
 * This replaces the standard polymorphic type deserializer for choice types
 * to handle primitive types (string, int, etc.) and nested choice types.
 */
public class ChoiceTypeDeserializerResolver extends AsPropertyTypeDeserializer {

    public ChoiceTypeDeserializerResolver(JavaType bt, String typePropertyName) {
        this(bt, createTypeIdResolver(bt), typePropertyName);
    }

    public ChoiceTypeDeserializerResolver(JavaType bt, TypeIdResolver idRes, String typePropertyName) {
        super(bt, idRes, typePropertyName, false, null, null);
    }

    private static TypeIdResolver createTypeIdResolver(JavaType baseType) {
        return new ChoiceTypeIdResolver(baseType, TypeFactory.defaultInstance());
    }

    protected ChoiceTypeDeserializerResolver(AsPropertyTypeDeserializer src, BeanProperty property) {
        super(src, property);
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return (prop == _property) ? this : new ChoiceTypeDeserializerResolver(this, prop);
    }

    @Override
    public Object deserializeTypedFromAny(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserializeTypedFromObject(p, ctxt);
    }

    @Override
    public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {

        // Read the object to inspect the @type field
        if (p.currentToken() != JsonToken.START_OBJECT) {
            ctxt.reportInputMismatch(_baseType, "Expected START_OBJECT for choice type");
            return null;
        }

        JsonNode node = p.readValueAsTree();

        if (!node.has(RuneJsonConfig.MetaProperties.TYPE)) {
            ctxt.reportInputMismatch(_baseType, "Missing @type field in choice type");
            return null;
        }

        String typeStr = node.get(RuneJsonConfig.MetaProperties.TYPE).asText();

        // Check if this is a primitive type
        if (isPrimitiveType(typeStr)) {
            if (!node.has(RuneJsonConfig.MetaProperties.DATA)) {
                ctxt.reportInputMismatch(_baseType, "Missing @data field for primitive choice type");
                return null;
            }

            Object primitiveValue = deserializePrimitive(typeStr, node.get(RuneJsonConfig.MetaProperties.DATA), ctxt);
            return wrapInChoiceType(primitiveValue, ctxt);
        }

        // It's a data type - deserialize it
        try {
            Class<?> targetClass = ctxt.findClass(typeStr);

            // Remove @type field and deserialize as the target type
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.remove(RuneJsonConfig.MetaProperties.TYPE);

            Object dataValue = ctxt.readTreeAsValue(objectNode, targetClass);
            return wrapInChoiceType(dataValue, ctxt);
        } catch (ClassNotFoundException e) {
            ctxt.reportInputMismatch(_baseType, "Could not resolve type: " + typeStr);
            return null;
        }
    }

    private boolean isPrimitiveType(String typeStr) {
        return "string".equals(typeStr) ||
               "int".equals(typeStr) ||
               "number".equals(typeStr) ||
               "boolean".equals(typeStr);
    }

    private Object deserializePrimitive(String typeStr, JsonNode valueNode, DeserializationContext ctxt) {
        switch (typeStr) {
            case "string":
                return valueNode.asText();
            case "int":
                // Handle int which can be Integer, Long, or BigInteger depending on size
                if (valueNode.canConvertToInt()) {
                    return valueNode.asInt();
                } else if (valueNode.canConvertToLong()) {
                    return valueNode.asLong();
                } else {
                    return valueNode.bigIntegerValue();
                }
            case "number":
                return valueNode.decimalValue();
            case "boolean":
                return valueNode.asBoolean();
            default:
                return null;
        }
    }

    private Object wrapInChoiceType(Object value, DeserializationContext ctxt) throws IOException {
        try {
            Class<?> choiceClass = _baseType.getRawClass();

            // The choice interface has a static builder() method
            java.lang.reflect.Method builderMethod = choiceClass.getMethod("builder");
            if (!java.lang.reflect.Modifier.isStatic(builderMethod.getModifiers())) {
                throw new IOException("builder() method is not static on choice type: " + choiceClass);
            }

            // Create builder instance
            Object builder = builderMethod.invoke(null);

            // The builder should have setter methods corresponding to choice options
            // For primitive types like String, look for setString(String)
            // For data types like A, look for setA(A)
            // For nested choices, the value might be an Impl class but the setter expects an interface

            // First, try to find the appropriate setter based on value type
            java.lang.reflect.Method setterMethod = null;
            Class<?> valueClass = value.getClass();

            // Get all setter methods on builder
            for (java.lang.reflect.Method m : builder.getClass().getMethods()) {
                String methodName = m.getName();
                if (methodName.startsWith("set") && m.getParameterCount() == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    // Check if this setter can accept our value directly
                    if (paramType.isAssignableFrom(valueClass)) {
                        setterMethod = m;
                        break;
                    }
                }
            }

            // If no direct setter found, check if the value's interfaces match any setter parameter types
            // This handles nested choices where A$AImpl implements A, and the setter expects ChoiceData
            if (setterMethod == null) {
                // Get the interfaces implemented by the value
                Class<?>[] valueInterfaces = valueClass.getInterfaces();

                for (java.lang.reflect.Method m : builder.getClass().getMethods()) {
                    String methodName = m.getName();
                    if (methodName.startsWith("set") && m.getParameterCount() == 1) {
                        Class<?> paramType = m.getParameterTypes()[0];

                        // Check if the parameter type is a choice type that can wrap our value
                        if (paramType.isInterface() && paramType.getSimpleName().contains("Choice")) {
                            // Try to wrap the value in this intermediate choice type
                            try {
                                Object wrappedValue = wrapInIntermediateChoice(value, paramType, ctxt);
                                if (wrappedValue != null) {
                                    setterMethod = m;
                                    value = wrappedValue; // Use the wrapped value
                                    break;
                                }
                            } catch (Exception e) {
                                // Not the right intermediate type, continue
                            }
                        }
                    }
                }
            }

            if (setterMethod == null) {
                throw new IOException("Could not find setter method for value type: " + valueClass + " in builder: " + builder.getClass());
            }

            // Set the value
            setterMethod.invoke(builder, value);

            // Build and return
            java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
            return buildMethod.invoke(builder);

        } catch (Exception e) {
            throw new IOException("Failed to wrap value in choice type: " + e.getMessage(), e);
        }
    }

    private Object wrapInIntermediateChoice(Object value, Class<?> intermediateChoiceType, DeserializationContext ctxt) throws Exception {
        // Get the builder for the intermediate choice type
        java.lang.reflect.Method builderMethod = intermediateChoiceType.getMethod("builder");
        Object builder = builderMethod.invoke(null);

        // Try to find a setter that accepts the value
        for (java.lang.reflect.Method m : builder.getClass().getMethods()) {
            if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isAssignableFrom(value.getClass())) {
                    m.invoke(builder, value);
                    java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
                    return buildMethod.invoke(builder);
                }
            }
        }

        return null;
    }
}
