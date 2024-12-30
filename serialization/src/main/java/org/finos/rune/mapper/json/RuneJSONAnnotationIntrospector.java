package org.finos.rune.mapper.json;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.rosetta.model.lib.annotations.*;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RuneJSONAnnotationIntrospector extends JacksonAnnotationIntrospector {
    private static final long serialVersionUID = 1L;

    private final RuneEnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector;

    private final RuneEnumBuilderIntrospector rosettaEnumBuilderIntrospector;

    public RuneJSONAnnotationIntrospector(boolean supportRosettaEnumValue) {
        this(new RuneEnumAsStringBuilderIntrospector(), new RuneEnumBuilderIntrospector(supportRosettaEnumValue));
    }

    public RuneJSONAnnotationIntrospector(RuneEnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector, RuneEnumBuilderIntrospector rosettaEnumBuilderIntrospector) {
        this.rosettaEnumBuilderIntrospector = rosettaEnumBuilderIntrospector;
        this.enumAsStringBuilderIntrospector = enumAsStringBuilderIntrospector;
    }


    @Override
    protected StdTypeResolverBuilder _constructStdTypeResolverBuilder() {
        return new RuneStdTypeResolverBuilder();
    }

    @Override
    protected TypeResolverBuilder<?> _constructStdTypeResolverBuilder(MapperConfig<?> config,
                                                                      JsonTypeInfo.Value typeInfo, JavaType baseType) {
        return new RuneStdTypeResolverBuilder(typeInfo);
    }

    @Override
    public JsonTypeInfo.Value findPolymorphicTypeInfo(MapperConfig<?> config, Annotated ann) {
        RuneDataType t = _findAnnotation(ann, RuneDataType.class);
        if (t != null) {
            return JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CLASS,
                    JsonTypeInfo.As.EXISTING_PROPERTY,
                    "@type",
                    JsonTypeInfo.class,
                    true,
                    false);
        }
        return super.findPolymorphicTypeInfo(config, ann);
    }

    //TODO: find out why this isn't working
    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RuneDataType.class)) {
            return new String[]{"@model", "@type", "@version", "@scheme"};
        }
        return super.findSerializationPropertyOrder(ac);
    }

    @Override
    public Boolean findSerializationSortAlphabetically(Annotated ann) {
        if (ann.hasAnnotation(RuneDataType.class)) {
            return Boolean.TRUE;
        }
        return super.findSerializationSortAlphabetically(ann);
    }

    @Override
    protected StdTypeResolverBuilder _constructNoTypeResolverBuilder() {
        return RuneStdTypeResolverBuilder.noTypeInfoBuilder();
    }


    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RuneDataType.class)) {
            return ac.getAnnotation(RuneDataType.class).builder();
        }
        return super.findPOJOBuilder(ac);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        if (a.hasAnnotation(RuneAttribute.class)) {
            return new PropertyName(a.getAnnotation(RuneAttribute.class).value());
        }
        return super.findNameForSerialization(a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        if (a.hasAnnotation(RuneAttribute.class)) {
            return new PropertyName(a.getAnnotation(RuneAttribute.class).value());
        }
        return super.findNameForDeserialization(a);
    }

    @Override
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass enumType,
                                   Enum<?>[] enumValues, String[] names) {
        if (rosettaEnumBuilderIntrospector.isApplicable(enumType)) {
            rosettaEnumBuilderIntrospector.findEnumValues(enumType, enumValues, names);
        } else {
            enumAsStringBuilderIntrospector.findEnumValues(enumType, enumValues, names);
        }
        return names;
    }

    @Override
    public void findEnumAliases(MapperConfig<?> config, AnnotatedClass enumType,
                                Enum<?>[] enumValues, String[][] aliasList) {
        if (rosettaEnumBuilderIntrospector.isApplicable(enumType)) {
            rosettaEnumBuilderIntrospector.findEnumAliases(enumType, enumValues, aliasList);
        } else {
            super.findEnumAliases(config, enumType, enumValues, aliasList);
        }
    }

    @Override
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
        RuneMetaType ann = _findAnnotation(member, RuneMetaType.class);
        // if not enabled, just means annotation is not enabled; not necessarily
        // that unwrapping should not be done (relevant when using chained introspectors)
        if (ann == null) {
            return super.findUnwrappingNameTransformer(member);
        }
        return  NameTransformer.NOP;
    }

    @Override
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated a) {
        if (a instanceof AnnotatedClass && a.hasAnnotation(RuneDataType.class)) {
            AnnotatedClass acc = (AnnotatedClass) a;
            Set<String> includes = getPropertyNames(acc, x -> x.hasAnnotation(RuneAttribute.class));
            Set<String> ignored = getPropertyNames(acc, x -> !x.hasAnnotation(RuneAttribute.class));
            ignored.removeAll(includes);
            return JsonIgnoreProperties.Value.forIgnoredProperties(ignored).withAllowSetters();
        }

        return super.findPropertyIgnoralByName(config, a);
    }

    private static Set<String> getPropertyNames(AnnotatedClass acc, Predicate<AnnotatedMethod> filter) {
        return StreamSupport.stream(acc.memberMethods().spliterator(), false)
                .filter(filter)
                .map(m -> RuneBeanUtil.getPropertyName(m.getAnnotated()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

}
