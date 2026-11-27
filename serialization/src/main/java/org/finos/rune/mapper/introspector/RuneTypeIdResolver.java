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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.finos.rune.mapper.RuneTypeNameConverter;

import java.io.IOException;
import java.util.Collection;

/**
 * Reads {@code @type} as the qualified Rune name of a type, accepting no other spelling of it.
 * {@link RuneStdTypeResolverBuilder} installs it for the polymorphic Rune data types that
 * {@link RuneJsonAnnotationIntrospector} gives type info.
 * <p>
 * Only reading is overridden: {@code @type} is written as an existing property, from {@code getType()},
 * so the id Jackson computes on write is discarded.
 */
public class RuneTypeIdResolver extends ClassNameIdResolver {

    private static final long serialVersionUID = 1L;

    public RuneTypeIdResolver(JavaType baseType, TypeFactory typeFactory, Collection<NamedType> subtypes,
                              PolymorphicTypeValidator subtypeValidator) {
        super(baseType, typeFactory, subtypes, subtypeValidator);
    }

    @Override
    protected JavaType _typeFromId(String id, DatabindContext ctxt) throws IOException {
        String javaClassName = RuneTypeNameConverter.toJavaClassName(id);
        JavaType type;
        try {
            type = ctxt.resolveAndValidateSubType(_baseType, javaClassName, _subTypeValidator);
        } catch (InvalidTypeIdException e) {
            if (javaClassName.equals(id)) {
                throw e;
            }
            JsonParser parser = ctxt instanceof DeserializationContext ? ((DeserializationContext) ctxt).getParser() : null;
            InvalidTypeIdException renamed = InvalidTypeIdException.from(
                    parser, e.getOriginalMessage().replace(javaClassName, id), _baseType, id);
            renamed.withCause(e);
            throw renamed;
        }
        // Rejects any other spelling that still loads the class, such as the Java name a._new.Root.
        if (type != null && id.equals(RuneTypeNameConverter.toRuneName(type.getRawClass()))) {
            return type;
        }
        if (ctxt instanceof DeserializationContext) {
            return ((DeserializationContext) ctxt).handleUnknownTypeId(_baseType, id, this, "no Rune type with this name");
        }
        return null;
    }
}
