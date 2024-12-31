package org.finos.rune.mapper.introspector;

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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.rosetta.model.lib.annotations.RuneDataType;

public class RuneStdTypeResolverBuilder extends StdTypeResolverBuilder {

    public RuneStdTypeResolverBuilder(JsonTypeInfo.Value typeInfo) {
        super(typeInfo);
    }

    public RuneStdTypeResolverBuilder() {
        super();
    }

    @Override
    public boolean _strictTypeIdHandling(DeserializationConfig config, JavaType baseType) {
        AnnotatedClass annotatedClass = AnnotatedClassResolver.resolve(config, baseType, config);
        if (annotatedClass.hasAnnotation(RuneDataType.class)) {
            return _requireTypeIdForSubtypes;
        }

        return super._strictTypeIdHandling(config, baseType);
    }
}
