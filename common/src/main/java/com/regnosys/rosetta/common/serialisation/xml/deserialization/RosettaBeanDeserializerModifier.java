package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMapLoader;
import com.regnosys.rosetta.common.serialisation.xml.RosettaXMLTypeConfigLookup;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Support for deserialising substitution groups by adding additional properties
 * to the `BeanDeserializerBuilder` for each additional name of a substitution. Also wraps the
 * generated deserializer with a {@link XMLContentModelDisambiguatingDeserializer} for Rosetta
 * types whose XML configuration provides a {@code contentModel}.
 */
public class RosettaBeanDeserializerModifier extends BeanDeserializerModifier {
    private final SubstitutionMapLoader substitutionMapLoader;
    private final RosettaXMLConfiguration rosettaXMLConfiguration;
    private final VirtualPathBuilderHelper virtualPathBuilderHelper;

    public RosettaBeanDeserializerModifier(SubstitutionMapLoader substitutionMapLoader,
                                           RosettaXMLConfiguration rosettaXMLConfiguration) {
        this.substitutionMapLoader = substitutionMapLoader;
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.virtualPathBuilderHelper = new VirtualPathBuilderHelper();
    }

    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
                                                 BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        addSubstitutionProperties(config, builder, intr);
        return builder;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                  BeanDescription beanDesc,
                                                  JsonDeserializer<?> deserializer) {
        Optional<TypeXMLConfiguration> typeConfig = RosettaXMLTypeConfigLookup.getTypeXMLConfiguration(
                rosettaXMLConfiguration, config, beanDesc);
        if (!typeConfig.isPresent()) {
            return deserializer;
        }
        Optional<XMLContentModel> contentModel = typeConfig.get().getContentModel();
        if (!contentModel.isPresent()) {
            return deserializer;
        }
        if (!XMLContentModelDisambiguatingDeserializer.requiresRouting(contentModel.get())) {
            return deserializer;
        }
        return new XMLContentModelDisambiguatingDeserializer(
                deserializer,
                beanDesc.getBeanClass(),
                typeConfig.get(),
                contentModel.get(),
                virtualPathBuilderHelper);
    }

    private void addSubstitutionProperties(DeserializationConfig config,
                                           BeanDeserializerBuilder builder,
                                           AnnotationIntrospector intr) {
        List<PropertyName> propNames = new ArrayList<>();
        builder.getProperties().forEachRemaining(p -> propNames.add(p.getFullName()));
        for (PropertyName propName : propNames) {
            SettableBeanProperty prop = builder.findProperty(propName);
            // Skip if already a SubstitutedMethodProperty to avoid double-processing
//            if (!(prop instanceof MethodProperty)) {
//                continue;
//            }

            AnnotatedMember acc = prop.getMember();
            SubstitutionMap substitutionMap = substitutionMapLoader.findSubstitutionMap(config, intr, acc);
            if (substitutionMap != null) {
                for (JavaType substitutedType : substitutionMap.getTypes()) {
                    String substitutedName = substitutionMap.getName(substitutedType);
                    SettableBeanProperty substitutedProperty = new SubstitutedMethodProperty((MethodProperty)prop, substitutedType, (AnnotatedMethod) acc, substitutionMap).withSimpleName(substitutedName);
                    // TODO: only replace the original property - make sure we are not accidentally replacing random other properties
                    builder.addOrReplaceProperty(substitutedProperty, true);
                }
            }
        }
    }
}
