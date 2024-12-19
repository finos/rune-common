package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

import java.util.ArrayList;
import java.util.List;

import static com.regnosys.rosetta.common.serialisation.xml.RosettaBeanSerializerModifier.findSubstitutionMap;

/**
 * Support for deserialising substitution groups by adding additional properties
 * to the `BeanDeserializerBuilder` for each additional name of a substitution.
 */
public class RosettaBeanDeserializerModifier extends BeanDeserializerModifier {
    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
                                                 BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        List<PropertyName> propNames = new ArrayList<>();
        builder.getProperties().forEachRemaining(p -> propNames.add(p.getFullName()));
        for (PropertyName propName : propNames) {
            SettableBeanProperty prop = builder.findProperty(propName);

            AnnotatedMember acc = prop.getMember();
            SubstitutionMap substitutionMap = findSubstitutionMap(config, intr, acc);
            if (substitutionMap != null) {
                for (JavaType substitutedType : substitutionMap.getTypes()) {
                    String substitutedName = substitutionMap.getName(substitutedType);
                    SettableBeanProperty substitutedProperty = new SubstitutedMethodProperty((MethodProperty)prop, substitutedType, (AnnotatedMethod) acc).withSimpleName(substitutedName);
                    // TODO: only replace the original property - make sure we are not accidentally replacing random other properties
                    builder.addOrReplaceProperty(substitutedProperty, true);
                }
            }
        }
        return builder;
    }
}
