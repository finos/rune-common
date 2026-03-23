package org.finos.rune.mapper.filters;

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
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneAttribute;

import java.lang.reflect.Method;

public class SubtypeFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
        String name = writer.getName();
        if (name.equals("@type")) {
            /*
                Include @type only when needed for polymorphic deserialization:
                - When the object's runtime class is a subtype AND
                - The object is being serialized into a property expecting a base type
                This allows proper deserialization of subtypes when assigned to base type fields
             */
            Class<?> runtimeClass = pojo.getClass();
            Class<?> superclass = runtimeClass.getSuperclass();

            if (superclass != null && !superclass.equals(Object.class)) {
                JsonStreamContext parentContext = jgen.getOutputContext().getParent();
                if (parentContext != null) {
                    String propertyName = parentContext.getCurrentName();
                    Object parentObject = parentContext.getCurrentValue();

                    if (propertyName != null && parentObject != null) {
                        try {
                            Class<?> parentClass = parentObject.getClass();
                            Method getter = findMethod(parentClass, propertyName);

                            if (getter != null) {
                                Class<?> declaredType = getter.getReturnType();
                                if (declaredType.isAssignableFrom(runtimeClass)) {
                                    if (pojo instanceof RosettaModelObject) {
                                        Class<? extends RosettaModelObject> type = ((RosettaModelObject) pojo).getType();
                                        if (declaredType.equals(type)) {
                                            return;
                                        }
                                    }
                                    writer.serializeAsField(pojo, jgen, provider);
                                }
                            }
                        } catch (Exception e) {
                            // If we can't determine the declared type, don't include @type
                        }
                    }
                }
            }
            return;
        }
        super.serializeAsField(pojo, jgen, provider, writer);
    }

    private Method findMethod(Class<?> clazz, String fieldName) {
        for (Method method : clazz.getMethods()) {
            RuneAttribute annotation = method.getAnnotation(RuneAttribute.class);
            if (annotation != null && annotation.value().equals(fieldName)) {
                return method;
            }
        }
        return null;
    }
}
