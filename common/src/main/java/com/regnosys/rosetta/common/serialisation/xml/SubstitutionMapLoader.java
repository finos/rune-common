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

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;

public class SubstitutionMapLoader {
    private final ClassLoader classLoader;
    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    public SubstitutionMapLoader(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public SubstitutionMapLoader(ClassLoader classLoader, RosettaXMLConfiguration rosettaXMLConfiguration) {
        this.classLoader = classLoader;
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
    }

    public SubstitutionMap findSubstitutionMap(MapperConfig<?> config,
                                                      AnnotationIntrospector ai,
                                                      AnnotatedMember prop) {
        for (AnnotationIntrospector intr : ai.allIntrospectors()) {
            if (intr instanceof RosettaXMLAnnotationIntrospector) {
                SubstitutionMap sm = ((RosettaXMLAnnotationIntrospector) intr).findSubstitutionMap(config, prop, classLoader);
                if (sm != null) {
                    return sm;
                }
            }
        }
        return null;
    }

    /**
     * Extracts the namespace from the XML configuration for a given type.
     * The namespace is derived from the xmlElementFullyQualifiedName (format: "namespace/localName").
     */
    public String getNamespaceForType(MapperConfig<?> config, JavaType type) {
        if (rosettaXMLConfiguration == null) {
            return null;
        }

        try {
            ModelSymbolId symbolId = ModelSymbolId.fromQualifiedName(type.getRawClass().getName());
            return rosettaXMLConfiguration.getConfigurationForType(symbolId)
                    .flatMap(typeConfig -> typeConfig.getXmlElementFullyQualifiedName())
                    .map(fqn -> {
                        int lastSlash = fqn.lastIndexOf('/');
                        return lastSlash > 0 ? fqn.substring(0, lastSlash) : null;
                    })
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
