package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.util.DottedPath;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the {@link TypeXMLConfiguration} for an arbitrary Jackson bean class.
 *
 * <p>The resolution must work for both the Rosetta interface types (annotated with
 * {@link RosettaDataType}) and the generated nested builder/implementation classes, which inherit
 * the annotation through the declaring class hierarchy and/or implemented interfaces.</p>
 */
final class RosettaXMLTypeConfigResolver {

    private final RosettaXMLConfiguration xmlConfiguration;

    RosettaXMLTypeConfigResolver(RosettaXMLConfiguration xmlConfiguration) {
        this.xmlConfiguration = xmlConfiguration;
    }

    Optional<TypeXMLConfiguration> getConfigForBean(BeanDescription beanDesc) {
        if (beanDesc == null) {
            return Optional.empty();
        }
        return getConfigForBeanClass(beanDesc.getBeanClass());
    }

    private Optional<TypeXMLConfiguration> getConfigForBeanClass(Class<?> beanClass) {
        return getModelSymbolIdForBean(beanClass).flatMap(xmlConfiguration::getConfigurationForType);
    }

    private Optional<ModelSymbolId> getModelSymbolIdForBean(Class<?> beanClass) {
        if (beanClass == null) {
            return Optional.empty();
        }
        // 1. Direct annotation on the bean class.
        ModelSymbolId direct = symbolFromTypeOrInterfaces(beanClass, new HashSet<>());
        if (direct != null) {
            return Optional.of(direct);
        }
        // 2. Walk declaring classes (handles generated nested builders/impls).
        Class<?> declaring = beanClass.getDeclaringClass();
        while (declaring != null) {
            ModelSymbolId fromDeclaring = symbolFromTypeOrInterfaces(declaring, new HashSet<Class<?>>());
            if (fromDeclaring != null) {
                return Optional.of(fromDeclaring);
            }
            declaring = declaring.getDeclaringClass();
        }
        // 3. Walk superclasses.
        Class<?> superclass = beanClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return getModelSymbolIdForBean(superclass);
        }
        return Optional.empty();
    }

    private static ModelSymbolId symbolFromTypeOrInterfaces(Class<?> type, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        ModelSymbolId direct = symbolFromAnnotation(type);
        if (direct != null) {
            return direct;
        }
        for (Class<?> iface : type.getInterfaces()) {
            ModelSymbolId fromIface = symbolFromTypeOrInterfaces(iface, visited);
            if (fromIface != null) {
                return fromIface;
            }
        }
        return null;
    }

    private static ModelSymbolId symbolFromAnnotation(Class<?> type) {
        RosettaDataType annotation = type.getAnnotation(RosettaDataType.class);
        if (annotation == null) {
            return null;
        }
        Package pkg = type.getPackage();
        String packageName = pkg == null ? "" : pkg.getName();
        return new ModelSymbolId(DottedPath.splitOnDots(packageName), annotation.value());
    }
}
