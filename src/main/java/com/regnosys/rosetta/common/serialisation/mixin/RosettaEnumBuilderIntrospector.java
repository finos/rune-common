package com.regnosys.rosetta.common.serialisation.mixin;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class RosettaEnumBuilderIntrospector {

    private final EnumNameFunc enumNameFunc;
    private final EnumAliasFunc enumAliasFunc;

    public RosettaEnumBuilderIntrospector(boolean supportRosettaEnumValue) {
        if (supportRosettaEnumValue) {
            this.enumNameFunc = (annotation, javaEnumName) -> annotation.value();
        } else {
            this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().equals("") ? annotation.displayName() : javaEnumName;
        }
        this.enumAliasFunc = (annotation, javaEnumName) -> !annotation.displayName().equals("") ?
                new String[]{javaEnumName, annotation.displayName(), annotation.value()}:
                new String[]{javaEnumName,  annotation.value()};
    }

    public boolean isApplicable(Class<?> enumType) {
        return enumType.getAnnotation(RosettaEnum.class) != null;
    }

    public void findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .filter(f -> f.getAnnotation(RosettaEnumValue.class) != null)
                .forEach(f -> {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            names[i] = enumNameFunc.apply(annotation, name);
                        }
                    }
                });
    }

    public void findEnumAliases(Class<?> enumType, Enum<?>[] enumValues, String[][] aliasList) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .filter(f -> f.getAnnotation(RosettaEnumValue.class) != null)
                .forEach(f -> {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            aliasList[i] = enumAliasFunc.apply(annotation, name);
                        }
                    }
                });
    }


    interface EnumNameFunc extends BiFunction<RosettaEnumValue, String, String> {

    }

    interface EnumAliasFunc extends BiFunction<RosettaEnumValue, String, String[]> {

    }
}
