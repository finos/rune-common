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

            // Check if this is a subtype (has a non-Object superclass)
            if (superclass != null && !superclass.equals(Object.class)) {
                // Get the property name from the JSON stream context (parent context)
                JsonStreamContext parentContext = jgen.getOutputContext().getParent();
                if (parentContext != null) {
                    String propertyName = parentContext.getCurrentName();
                    Object parentObject = parentContext.getCurrentValue();

                    if (propertyName != null && parentObject != null) {
                        // Try to find the declared type of the property in the parent object
                        try {
                            Class<?> parentClass = parentObject.getClass();
                            // Look for getter method that matches the property
                            String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                            java.lang.reflect.Method getter = findMethod(parentClass, getterName);

                            if (getter != null) {
                                Class<?> declaredType = getter.getReturnType();
                                // Include @type only if the object's type is a proper subtype of the declared type
                                // This means:
                                // 1. The declared type is assignable from runtime class (declared is supertype)
                                // 2. BUT the runtime class's direct superclass or implemented interface
                                //    is different from the declared type
                                //
                                // Example:
                                // - typeA is declared as A, contains B (B extends A) -> Include @type (polymorphic)
                                // - typeB is declared as B, contains B -> Don't include @type (not polymorphic)
                                //
                                // For generated Rosetta types:
                                // - Runtime class is like B$BImpl (implements B interface)
                                // - Declared type is B interface
                                // - We need to check if B is the direct interface or a parent interface

                                if (declaredType.isAssignableFrom(runtimeClass)) {
                                    // Check if the declared type is a parent (not the direct type)
                                    boolean isDirectType = false;

                                    // Check if runtime class directly implements the declared interface
                                    for (Class<?> iface : runtimeClass.getInterfaces()) {
                                        if (iface.equals(declaredType)) {
                                            isDirectType = true;
                                            break;
                                        }
                                    }

                                    // If not direct, it's polymorphic - include @type
                                    if (!isDirectType) {
                                        writer.serializeAsField(pojo, jgen, provider);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // If we can't determine the declared type, don't include @type
                            // This is the safe fallback to avoid adding @type everywhere
                        }
                    }
                }
            }
            return;
        }
        super.serializeAsField(pojo, jgen, provider, writer);
    }

    private java.lang.reflect.Method findMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            // Try superclass
            if (clazz.getSuperclass() != null) {
                return findMethod(clazz.getSuperclass(), methodName);
            }
            return null;
        }
    }


}
