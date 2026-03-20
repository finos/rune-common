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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Custom TypeIdResolver for choice types that can resolve both:
 * 1. Primitive type names (string, int, number, etc.)
 * 2. Fully qualified class names (for data types)
 */
public class ChoiceTypeIdResolver extends TypeIdResolverBase {

    private final JavaType baseType;

    public ChoiceTypeIdResolver(JavaType baseType, TypeFactory typeFactory) {
        super(baseType, typeFactory);
        this.baseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        if (value == null) {
            return null;
        }

        // For primitives, return simple type name
        if (value instanceof String) {
            return "string";
        } else if (value instanceof Integer) {
            return "int";
        } else if (value instanceof Long) {
            return "long";
        } else if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        }

        // For data types, return fully qualified class name
        return value.getClass().getName();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return idFromValue(value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        if (id == null || id.isEmpty()) {
            return null;
        }

        // Handle primitive types
        switch (id) {
            case "string":
                return _typeFactory.constructType(String.class);
            case "int":
                return _typeFactory.constructType(Integer.class);
            case "long":
                return _typeFactory.constructType(Long.class);
            case "number":
                return _typeFactory.constructType(BigDecimal.class);
            case "boolean":
                return _typeFactory.constructType(Boolean.class);
            default:
                // Try to resolve as a class name
                try {
                    Class<?> cls = null;
                    if (context instanceof DeserializationContext) {
                        cls = ((DeserializationContext) context).findClass(id);
                    } else {
                        // Fallback to classloader
                        cls = Class.forName(id);
                    }
                    return _typeFactory.constructType(cls);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Cannot resolve type id '" + id + "'", e);
                }
        }
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
