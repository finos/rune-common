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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Custom serializer for choice types that flattens the choice wrapper.
 *
 * When serializing a choice type, instead of wrapping it in an extra layer,
 * this serializer directly serializes the contained value with appropriate
 * type information.
 */
public class ChoiceTypeSerializer extends StdSerializer<Object> {

    public ChoiceTypeSerializer() {
        super(Object.class);
    }

    @SuppressWarnings("unchecked")
    public ChoiceTypeSerializer(Class<?> t) {
        super((Class<Object>) t);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // Get the actual value from the choice wrapper
        Object actualValue = extractChoiceValue(value);

        if (actualValue == null) {
            gen.writeNull();
            return;
        }

        // Start object for choice value
        gen.writeStartObject();

        // Determine the type and serialize accordingly
        if (actualValue instanceof RosettaModelObject) {
            // For data types, write @type and flatten the fields
            // Get the interface name, not the implementation class name
            String className = getInterfaceName(actualValue);
            gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, className);

            // Serialize the data type fields
            JsonSerializer<Object> serializer = provider.findValueSerializer(actualValue.getClass());
            serializer.unwrappingSerializer(null).serialize(actualValue, gen, provider);
        } else {
            // For primitives, write @type and @data
            String typeName = getSimpleTypeName(actualValue);
            gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, typeName);
            gen.writeObjectField(RuneJsonConfig.MetaProperties.DATA, actualValue);
        }

        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        // For choice types, we handle type information ourselves
        serialize(value, gen, provider);
    }

    private Object extractChoiceValue(Object choiceWrapper) {
        try {
            // Try to get the value via reflection
            // Choice types typically have a single field or a getValue() method
            java.lang.reflect.Method[] methods = choiceWrapper.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().startsWith("get") &&
                    !method.getName().equals("getClass") &&
                    !method.getName().equals("getMeta") &&
                    !method.getName().equals("getType") &&
                    method.getParameterCount() == 0) {
                    Object result = method.invoke(choiceWrapper);
                    if (result != null) {
                        // Check if the result is itself a choice type (nested choice)
                        // If so, recursively extract its value
                        if (isChoiceType(result)) {
                            return extractChoiceValue(result);
                        }
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            // Fall back to returning the wrapper itself
        }
        return choiceWrapper;
    }

    private boolean isChoiceType(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?>[] interfaces = obj.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            if (iface.getSimpleName().contains("Choice") && !iface.getSimpleName().endsWith("Builder")) {
                return true;
            }
        }
        return false;
    }

    private String getSimpleTypeName(Object value) {
        if (value instanceof String) {
            return "string";
        } else if (value instanceof Integer) {
            return "int";
        } else if (value instanceof Long) {
            return "long";
        } else if (value instanceof Double || value instanceof Float) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        }
        return value.getClass().getSimpleName().toLowerCase();
    }

    private String getInterfaceName(Object value) {
        // Get the first interface that is not a common library interface
        Class<?>[] interfaces = value.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            String ifaceName = iface.getName();
            // Skip common library interfaces
            if (!ifaceName.startsWith("com.rosetta.model.lib") &&
                !ifaceName.startsWith("java.") &&
                !ifaceName.endsWith("Builder")) {
                return ifaceName;
            }
        }
        // Fall back to class name without the "Impl" suffix
        String className = value.getClass().getName();
        if (className.endsWith("Impl")) {
            className = className.substring(0, className.length() - 4);
        }
        // Remove inner class suffix like $AImpl
        int dollarIndex = className.lastIndexOf('$');
        if (dollarIndex > 0 && className.substring(dollarIndex).endsWith("Impl")) {
            className = className.substring(0, dollarIndex);
        }
        return className;
    }
}
