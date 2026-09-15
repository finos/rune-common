package com.regnosys.rosetta.common.serialisation.xml.serialization;

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
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanSerializer;
import com.regnosys.rosetta.common.serialisation.xml.RosettaXMLTypeConfigLookup;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMapLoader;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;

import java.util.List;
import java.util.Optional;

/**
 * Support for serialising substitution groups by replacing property writers
 * with {@code SubstitutingBeanPropertyWriter}, which will serialise the right
 * property name based on the type of the value. Also installs an
 * {@link XMLContentModelOrderer} on the {@link RosettaBeanSerializer} for types whose XML
 * configuration provides a {@code contentModel}, so that child elements are emitted in XSD order.
 */
public class RosettaBeanSerializerModifier extends BeanSerializerModifier {
    private final SubstitutionMapLoader substitutionMapLoader;
    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    public RosettaBeanSerializerModifier(SubstitutionMapLoader substitutionMapLoader,
                                         RosettaXMLConfiguration rosettaXMLConfiguration) {
        this.substitutionMapLoader = substitutionMapLoader;
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0, len = beanProperties.size(); i < len; ++i) {
            BeanPropertyWriter bpw = beanProperties.get(i);
            final AnnotatedMember member = bpw.getMember();
            SubstitutionMap substitutionMap = substitutionMapLoader.findSubstitutionMap(config, intr, member);
            if (substitutionMap != null) {
                beanProperties.set(i, new SubstitutingBeanPropertyWriter(bpw, substitutionMap));
            }
        }
        return beanProperties;
    }



    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                              BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (!(serializer instanceof XmlBeanSerializer)) {
            return serializer;
        }

        XMLContentModelOrderer contentModelOrderer = null;
        Optional<TypeXMLConfiguration> typeConfig =
                RosettaXMLTypeConfigLookup.getTypeXMLConfiguration(rosettaXMLConfiguration, config, beanDesc);
        if (typeConfig.isPresent()) {
            Optional<XMLContentModel> contentModel = typeConfig.get().getContentModel();
            if (contentModel.isPresent()) {
                contentModelOrderer = new XMLContentModelOrderer(contentModel.get());
            }
        }

        return new RosettaBeanSerializer((XmlBeanSerializer) serializer, null, contentModelOrderer);
    }
}
