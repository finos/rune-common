package com.regnosys.rosetta.common.serialisation.mixin;

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

import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;

import java.lang.reflect.Field;
import java.util.stream.Stream;

public class EnumAsStringBuilderIntrospector {

    public void findEnumValues(AnnotatedClass enumType, Enum<?>[] enumValues, String[] names) {
        for (AnnotatedField f : enumType.fields()) {
            final String name = f.getName();
            for (int i = 0, end = enumValues.length; i < end; ++i) {
                if (name.equals(enumValues[i].name())) {
                    names[i] = enumValues[i].toString();
                }
            }
        }
    }
}
