package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertiesCollector;

import java.util.Collections;
import java.util.List;

public class RosettaBeanDescription extends BasicBeanDescription {
    protected RosettaBeanDescription(POJOPropertiesCollector coll, JavaType type, AnnotatedClass classDef) {
        super(coll, type, classDef);
    }

    protected RosettaBeanDescription(MapperConfig<?> config, JavaType type, AnnotatedClass classDef, List<BeanPropertyDefinition> props) {
        super(config, type, classDef, props);
    }

    protected RosettaBeanDescription(POJOPropertiesCollector coll) {
        super(coll);
    }

    public static RosettaBeanDescription forDeserialization(POJOPropertiesCollector coll) {
        return new RosettaBeanDescription(coll);
    }

    public static RosettaBeanDescription forSerialization(POJOPropertiesCollector coll) {
        return new RosettaBeanDescription(coll);
    }

    public static RosettaBeanDescription forOtherUse(MapperConfig<?> config,
                                                     JavaType type, AnnotatedClass ac) {
        return new RosettaBeanDescription(config, type, ac, Collections.emptyList());
    }
}
