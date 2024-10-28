package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.POJOPropertiesCollector;
import com.rosetta.model.lib.annotations.RosettaDataType;

public class RosettaClassIntrospector extends BasicClassIntrospector {
    @Override
    public BasicBeanDescription forSerialization(SerializationConfig config,
                                                 JavaType type, MixInResolver r)
    {
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
                                                   JavaType type, MixInResolver r)
    {
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
                                                              JavaType builderType, MixInResolver r, BeanDescription valueTypeDesc)
    {
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
                                            JavaType type, MixInResolver r)
    {
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
                                                    JavaType type, MixInResolver r)
    {
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
                                                          JavaType type, MixInResolver r)
    {
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
