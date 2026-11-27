package org.finos.rune.mapper.introspector;

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

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.finos.rune.mapper.RuneTypeNameConverter;

import java.io.IOException;
import java.util.Collection;

/**
 * Resolves {@code @type} as the qualified Rune name of a type rather than its Java class name.
 */
public class RuneTypeIdResolver extends ClassNameIdResolver {

    private static final long serialVersionUID = 1L;

    public RuneTypeIdResolver(JavaType baseType, TypeFactory typeFactory, Collection<NamedType> subtypes,
                              PolymorphicTypeValidator subtypeValidator) {
        super(baseType, typeFactory, subtypes, subtypeValidator);
    }

    @Override
    protected String _idFrom(Object value, Class<?> cls, TypeFactory typeFactory) {
        return RuneTypeNameConverter.toRuneName(super._idFrom(value, cls, typeFactory));
    }

    @Override
    protected JavaType _typeFromId(String id, DatabindContext ctxt) throws IOException {
        return super._typeFromId(RuneTypeNameConverter.toJavaClassName(id), ctxt);
    }
}
