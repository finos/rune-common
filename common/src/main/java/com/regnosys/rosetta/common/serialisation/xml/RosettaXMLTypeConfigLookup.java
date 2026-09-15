package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.util.DottedPath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class RosettaXMLTypeConfigLookup {

    private RosettaXMLTypeConfigLookup() {
    }

    public static Optional<TypeXMLConfiguration> getTypeXMLConfiguration(RosettaXMLConfiguration rosettaXMLConfiguration,
                                                                         MapperConfig<?> config,
                                                                         BeanDescription beanDesc) {
        return getTypeXMLConfigurations(rosettaXMLConfiguration, config, beanDesc.getClassInfo()).stream()
                .findFirst();
    }

    public static List<TypeXMLConfiguration> getTypeXMLConfigurations(RosettaXMLConfiguration rosettaXMLConfiguration,
                                                                      MapperConfig<?> config,
                                                                      AnnotatedClass ac) {
        List<TypeXMLConfiguration> result = new ArrayList<>();
        Set<ModelSymbolId> visited = new HashSet<>();
        RosettaDataType ann;
        while ((ann = ac.getAnnotation(RosettaDataType.class)) != null) {
            final ModelSymbolId modelSymbolId = createModelSymbolId(ac, ann.value());
            if (visited.add(modelSymbolId)) {
                rosettaXMLConfiguration.getConfigurationForType(modelSymbolId).ifPresent(result::add);
            }

            if (ac.getType().getSuperClass() == null) {
                break;
            }
            ac = AnnotatedClassResolver.resolve(config, ac.getType().getSuperClass(), config);
        }
        return result;
    }

    public static AnnotatedClass getEnclosingAnnotatedClass(MapperConfig<?> config, AnnotatedMember member) {
        // TODO: see issue https://github.com/FasterXML/jackson-databind/issues/4141
        return AnnotatedClassResolver.resolve(config, config.constructType(member.getDeclaringClass()), config);
    }

    public static AnnotatedClass getAnnotatedClassOrContent(MapperConfig<?> config, AnnotatedMember member) {
        JavaType t;
        if (member instanceof AnnotatedMethod) {
            AnnotatedMethod method = (AnnotatedMethod) member;
            if (method.getParameterCount() == 1) {
                // For setters
                t = method.getParameterType(0);
            } else {
                t = method.getType();
            }
        } else {
            t = member.getType();
        }
        if (t.getContentType() != null) {
            t = t.getContentType();
        }
        return AnnotatedClassResolver.resolve(config, t, config);
    }

    private static ModelSymbolId createModelSymbolId(AnnotatedClass ac, String name) {
        final String namespace = ac.getType().getRawClass().getPackage().getName();
        return new ModelSymbolId(DottedPath.splitOnDots(namespace), name);
    }
}
