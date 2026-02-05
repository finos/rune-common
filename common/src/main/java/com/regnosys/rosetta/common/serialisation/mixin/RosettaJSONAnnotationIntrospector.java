package com.regnosys.rosetta.common.serialisation.mixin;

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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.module.blackbird.util.CheckedFunction;
import com.regnosys.rosetta.common.serialisation.BackwardsCompatibleAnnotationIntrospector;
import com.regnosys.rosetta.common.serialisation.BeanUtil;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyRosettaBuilderIntrospector;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RosettaJSONAnnotationIntrospector extends JacksonAnnotationIntrospector implements BackwardsCompatibleAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    private final LegacyRosettaBuilderIntrospector legacyRosettaBuilderIntrospector;
    private final EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector;

    private final RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector;

    public RosettaJSONAnnotationIntrospector(boolean supportRosettaEnumValue) {
        this(new LegacyRosettaBuilderIntrospector(), new EnumAsStringBuilderIntrospector(), new RosettaEnumBuilderIntrospector(supportRosettaEnumValue));
    }

    public RosettaJSONAnnotationIntrospector(LegacyRosettaBuilderIntrospector legacyRosettaBuilderIntrospector, EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector, RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector) {
        this.legacyRosettaBuilderIntrospector = legacyRosettaBuilderIntrospector;
        this.rosettaEnumBuilderIntrospector = rosettaEnumBuilderIntrospector;
        this.enumAsStringBuilderIntrospector = enumAsStringBuilderIntrospector;
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            return ac.getAnnotation(RosettaDataType.class).builder();
        }
        return legacyRosettaBuilderIntrospector.findPOJOBuilder(ac)
                .orElse(super.findPOJOBuilder(ac));
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            return new JsonPOJOBuilder.Value("build", "set");
        }
        return super.findPOJOBuilderConfig(ac);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        if (a.hasAnnotation(RosettaAttribute.class)) {
            return new PropertyName(a.getAnnotation(RosettaAttribute.class).value());
        }
        return super.findNameForSerialization(a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        if (a.hasAnnotation(RosettaAttribute.class)) {
            return new PropertyName(a.getAnnotation(RosettaAttribute.class).value());
        }
        return legacyRosettaBuilderIntrospector.findNameForDeserialization(a)
                .orElse(super.findNameForDeserialization(a));
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
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated ann) {
        return findPropertyIgnorals(ann);
    }

    @Deprecated
    @Override
    public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated ac) {
        if (ac instanceof AnnotatedClass && ac.hasAnnotation(RosettaDataType.class)) {
            AnnotatedClass acc = (AnnotatedClass) ac;
            Set<String> includes = getPropertyNames(acc, x -> shouldIncludeMethod(x));
            Set<String> ignored = getPropertyNames(acc, x -> !shouldIncludeMethod(x));
            ignored.removeAll(includes);
            return JsonIgnoreProperties.Value.forIgnoredProperties(ignored).withAllowSetters();
        }

        return legacyRosettaBuilderIntrospector.findPropertyIgnorals(ac)
                .orElse(JsonIgnoreProperties.Value.empty());
    }
    
    private boolean shouldIncludeMethod(AnnotatedMethod m) {
        return m.hasAnnotation(RosettaAttribute.class) && getAccessorType(m) != AccessorType.ADDER;
    }
    private AccessorType getAccessorType(Annotated m) {
        Accessor acc = m.getAnnotation(Accessor.class);
        return acc != null ? acc.value() : null;
    }

    private static Set<String> getPropertyNames(AnnotatedClass acc, Predicate<AnnotatedMethod> filter) {
        return StreamSupport.stream(acc.memberMethods().spliterator(), false)
                .filter(filter)
                .map(m -> {
                    RosettaAttribute attr = m.getAnnotation(RosettaAttribute.class);
                    if (attr != null && !attr.value().isEmpty()) {
                        return attr.value();
                    }
                    return BeanUtil.getPropertyName(m.getAnnotated());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // NOTE: `a.hasAnnotation(RosettaIgnore.class)` causes undeterministic property ordering for overridden attributes in the serialised object.
    @Override
    public boolean hasIgnoreMarker(AnnotatedMember a) {
        return a.getName().startsWith("add") || isMemberRosettaModelObjectGetTypeMethod(a) || a.hasAnnotation(RosettaIgnore.class) || super.hasIgnoreMarker(a);
    }

    private boolean isMemberRosettaModelObjectGetTypeMethod(AnnotatedMember am) {
        if (am.getMember() instanceof Method && am.getName().equals("getType")) {
            Method method = (Method) am.getMember();
            return RosettaModelObject.class.isAssignableFrom(method.getDeclaringClass());
        }
        return false;
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }
}
