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
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

public class SubstitutionMapLoader {
    private final ClassLoader classLoader;

    public SubstitutionMapLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
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
}
