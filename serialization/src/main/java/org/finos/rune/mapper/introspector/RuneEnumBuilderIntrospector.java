package org.finos.rune.mapper.introspector;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;

import java.util.function.BiFunction;

public class RuneEnumBuilderIntrospector {
    /*
     * TODO:
     *  The ENUM_ANNOTATION & ENUM_VALUE_ANNOTATION are marker annotations in our generated
     * POJOs that allow us to introspect Enumerations and transform their values correctly. All
     * serialisation annotations need to migrate away from using the Rosetta annotations to Rune ones.
     * The following two annotations need to be marked as deprecated in the DSL and the POJO generators
     * need to generate the replacement RuneEnum and RuneEnumValue annotations.
     */
    public static final Class<RosettaEnum> ENUM_ANNOTATION = RosettaEnum.class;
    public static final Class<RosettaEnumValue> ENUM_VALUE_ANNOTATION = RosettaEnumValue.class;
    private final EnumNameFunc enumNameFunc;
    private final EnumAliasFunc enumAliasFunc;

    public RuneEnumBuilderIntrospector() {
        this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ? annotation.displayName() : annotation.value();
        this.enumAliasFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ?
                new String[]{javaEnumName, annotation.displayName(), annotation.value()}:
                new String[]{javaEnumName,  annotation.value()};
    }

    public boolean isApplicable(AnnotatedClass enumType) {
        return enumType.getAnnotation(ENUM_ANNOTATION) != null;
    }

    public void findEnumValues(AnnotatedClass enumType, Enum<?>[] enumValues, String[] names) {
        processEnumAnnotations(enumType, enumValues, enumNameFunc, names);
    }

    public void findEnumAliases(AnnotatedClass enumType, Enum<?>[] enumValues, String[][] aliasList) {
        processEnumAnnotations(enumType, enumValues, enumAliasFunc, aliasList);
    }

    private <T> void processEnumAnnotations(
            AnnotatedClass enumType,
            Enum<?>[] enumValues,
            BiFunction<RosettaEnumValue, String, T> mapper,
            T[] results
    ) {
        for (AnnotatedField f : enumType.fields()) {
            if (f.hasAnnotation(ENUM_VALUE_ANNOTATION)) {
                RosettaEnumValue annotation = f.getAnnotation(ENUM_VALUE_ANNOTATION);
                final String name = f.getName();
                for (int i = 0, end = enumValues.length; i < end; ++i) {
                    if (name.equals(enumValues[i].name())) {
                        results[i] = mapper.apply(annotation, name);
                        break;
                    }
                }
            }
        }
    }

    private interface EnumNameFunc extends BiFunction<RosettaEnumValue, String, String> {

    }

    private interface EnumAliasFunc extends BiFunction<RosettaEnumValue, String, String[]> {

    }
}
