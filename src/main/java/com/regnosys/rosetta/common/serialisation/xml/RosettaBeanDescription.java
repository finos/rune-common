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
