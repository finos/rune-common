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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.rosetta.model.lib.annotations.RosettaDataType;

public class RosettaClassIntrospector extends BasicClassIntrospector {
    @Override
    public BasicBeanDescription forSerialization(SerializationConfig config,
                                                 JavaType type, MixInResolver r) {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(config, type);
            if (desc == null) {
                POJOPropertiesCollector collector = collectProperties(config, type, r, true);
                if (collector.getClassDef().hasAnnotation(RosettaDataType.class)) {
                    desc = RosettaBeanDescription.forSerialization(collector);
                } else {
                    desc = BasicBeanDescription.forSerialization(collector);
                }
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig config,
                                                   JavaType type, MixInResolver r) {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            // As per [Databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(config, type);
            if (desc == null) {
                POJOPropertiesCollector collector = collectProperties(config, type, r, false);
                if (collector.getClassDef().hasAnnotation(RosettaDataType.class)) {
                    desc = RosettaBeanDescription.forDeserialization(collector);
                } else {
                    desc = BasicBeanDescription.forDeserialization(collector);
                }
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDeserializationWithBuilder(DeserializationConfig config,
                                                              JavaType builderType, MixInResolver r, BeanDescription valueTypeDesc) {
        // no std JDK types with Builders, so:
        POJOPropertiesCollector collector = collectPropertiesWithBuilder(config,
                builderType, r, valueTypeDesc, false);
        if (collector.getClassDef().hasAnnotation(RosettaDataType.class)) {
            return RosettaBeanDescription.forDeserialization(collector);
        }
        return BasicBeanDescription.forDeserialization(collector);
    }

    @Override
    public BasicBeanDescription forCreation(DeserializationConfig config,
                                            JavaType type, MixInResolver r) {
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(config, type);
            if (desc == null) {
                POJOPropertiesCollector collector = collectProperties(config, type, r, false);
                if (collector.getClassDef().hasAnnotation(RosettaDataType.class)) {
                    desc = RosettaBeanDescription.forDeserialization(collector);
                } else {
                    desc = BasicBeanDescription.forDeserialization(collector);
                }
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forClassAnnotations(MapperConfig<?> config,
                                                    JavaType type, MixInResolver r) {
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            AnnotatedClass ac = _resolveAnnotatedClass(config, type, r);
            if (ac.hasAnnotation(RosettaDataType.class)) {
                desc = RosettaBeanDescription.forOtherUse(config, type,
                        ac);
            } else {
                desc = BasicBeanDescription.forOtherUse(config, type,
                        ac);
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription forDirectClassAnnotations(MapperConfig<?> config,
                                                          JavaType type, MixInResolver r) {
        BasicBeanDescription desc = _findStdTypeDesc(config, type);
        if (desc == null) {
            AnnotatedClass ac = _resolveAnnotatedWithoutSuperTypes(config, type, r);
            if (ac.hasAnnotation(RosettaDataType.class)) {
                desc = RosettaBeanDescription.forOtherUse(config, type,
                        ac);
            } else {
                desc = BasicBeanDescription.forOtherUse(config, type,
                        ac);
            }
        }
        return desc;
    }
}
