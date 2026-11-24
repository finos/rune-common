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
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap.XMLFullyQualifiedName;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMapLoader;
import com.regnosys.rosetta.common.serialisation.xml.XsiTypeValue;
import com.regnosys.rosetta.common.serialisation.xml.RosettaXMLTypeConfigLookup;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;

import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Support for deserialising substitution groups by adding additional properties
 * to the `BeanDeserializerBuilder` for each additional name of a substitution. Also wraps the
 * generated deserializer with a {@link XMLContentModelDisambiguatingDeserializer} for Rosetta
 * types whose XML configuration provides a {@code contentModel}, and with a {@link XsiTypeDeserializer}
 * for Rosetta types that have a subtype an {@code xsi:type} attribute can select.
 */
public class RosettaBeanDeserializerModifier extends BeanDeserializerModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(RosettaBeanDeserializerModifier.class);

    private final SubstitutionMapLoader substitutionMapLoader;
    private final RosettaXMLConfiguration rosettaXMLConfiguration;
    private final VirtualPathBuilderHelper virtualPathBuilderHelper;
    private volatile Map<XMLFullyQualifiedName, Class<?>> typesByXsdTypeName;

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
        // Only XsiTypeDeserializer reads it; every other type would see an unknown property.
        builder.addIgnorable(XsiTypeValue.FIELD_NAME);
        return builder;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                  BeanDescription beanDesc,
                                                  JsonDeserializer<?> deserializer) {
        JsonDeserializer<?> modified = wrapWithContentModelRouting(config, beanDesc, deserializer);
        return wrapWithXsiTypeSelection(beanDesc, modified);
    }

    private JsonDeserializer<?> wrapWithContentModelRouting(DeserializationConfig config,
                                                            BeanDescription beanDesc,
                                                            JsonDeserializer<?> deserializer) {
        Optional<TypeXMLConfiguration> typeConfig = RosettaXMLTypeConfigLookup.getTypeXMLConfigurationWithContentModel(
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

    private JsonDeserializer<?> wrapWithXsiTypeSelection(BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        Class<?> runeType = builtType(beanDesc.getBeanClass());
        Map<XMLFullyQualifiedName, Class<?>> selectable = new HashMap<>();
        getTypesByXsdTypeName().forEach((name, type) -> {
            if (runeType.isAssignableFrom(type)) {
                selectable.put(name, type);
            }
        });
        boolean hasSubtype = selectable.values().stream().anyMatch(type -> !type.equals(runeType));
        if (!hasSubtype) {
            return deserializer;
        }
        return new XsiTypeDeserializer(deserializer, runeType, Collections.unmodifiableMap(selectable));
    }

    /**
     * Rosetta types are read through their builders, so the bean being deserialized is a builder such as
     * {@code Shape.ShapeBuilderImpl}. Its covariant {@code build()} method returns the Rune type, {@code Shape}.
     */
    private static Class<?> builtType(Class<?> beanClass) {
        if (RosettaModelObjectBuilder.class.isAssignableFrom(beanClass)) {
            try {
                return beanClass.getMethod("build").getReturnType();
            } catch (NoSuchMethodException e) {
                return beanClass;
            }
        }
        return beanClass;
    }

    private Map<XMLFullyQualifiedName, Class<?>> getTypesByXsdTypeName() {
        Map<XMLFullyQualifiedName, Class<?>> result = typesByXsdTypeName;
        if (result == null) {
            result = new HashMap<>();
            ClassLoader classLoader = substitutionMapLoader.getClassLoader();
            for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> entry : rosettaXMLConfiguration.getTypeConfigMap().entrySet()) {
                Optional<String> xsdTypeName = entry.getValue().getXmlTypeFullyQualifiedName();
                if (!xsdTypeName.isPresent()) {
                    continue;
                }
                String className = entry.getKey().getQualifiedName().toString();
                try {
                    result.put(new XMLFullyQualifiedName(xsdTypeName.get()), classLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("The XML configuration gives the XSD type {} for {}, but that class cannot be loaded, "
                            + "so xsi:type cannot select it.", xsdTypeName.get(), className);
                }
            }
            typesByXsdTypeName = result;
        }
        return result;
    }

    private void addSubstitutionProperties(DeserializationConfig config,
                                           BeanDeserializerBuilder builder,
                                           AnnotationIntrospector intr) {
        List<PropertyName> propNames = new ArrayList<>();
        builder.getProperties().forEachRemaining(p -> propNames.add(p.getFullName()));
        for (PropertyName propName : propNames) {
            SettableBeanProperty prop = builder.findProperty(propName);
            // Skip if already a SubstitutedMethodProperty to avoid double-processing
            if (!(prop instanceof MethodProperty)) {
                continue;
            }

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
