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
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanPropertyWriter;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanSerializerBase;
import com.fasterxml.jackson.dataformat.xml.util.AnnotationUtil;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;
import com.fasterxml.jackson.dataformat.xml.util.XmlInfo;
import com.rosetta.model.lib.annotations.RosettaDataType;

import java.util.List;
import java.util.Map;

/**
 * Support for serialising substitution groups by replacing property writers
 * with {@code SubstitutingBeanPropertyWriter}, which will serialise the right
 * property name based on the type of the value.
 */
public class RosettaBeanSerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0, len = beanProperties.size(); i < len; ++i) {
            BeanPropertyWriter bpw = beanProperties.get(i);
            final AnnotatedMember member = bpw.getMember();
            SubstitutionMap substitutionMap = findSubstitutionMap(config, intr, member);
            if (substitutionMap != null) {
                beanProperties.set(i, new SubstitutingBeanPropertyWriter(bpw, substitutionMap));
            }
        }
        return beanProperties;
    }

    // TODO: move in common service
    public static SubstitutionMap findSubstitutionMap(MapperConfig<?> config,
                                                             AnnotationIntrospector ai,
                                                             AnnotatedMember prop) {
        for (AnnotationIntrospector intr : ai.allIntrospectors()) {
            if (intr instanceof RosettaXMLAnnotationIntrospector) {
                SubstitutionMap sm = ((RosettaXMLAnnotationIntrospector) intr).findSubstitutionMap(config, prop);
                if (sm != null) {
                    return sm;
                }
            }
        }
        return null;
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                              BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (!(serializer instanceof XmlBeanSerializer)) {
            return serializer;
        }
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();

        return new RosettaBeanSerializer((XmlBeanSerializer) serializer, null);
    }
}
