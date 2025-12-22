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
import com.google.common.collect.Lists;
import com.rosetta.model.lib.annotations.*;
import com.rosetta.model.metafields.MetaFields;
import org.finos.rune.mapper.RuneJsonConfig;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.rosetta.model.lib.SerializedNameConstants.*;

/**
 * Custom Jackson annotation introspector for handling serialization and deserialization
 * of classes annotated with {@link RuneDataType}, {@link RuneAttribute}, and {@link RuneMetaType}.
 *
 * <p>This introspector modifies Jackson's default behavior to:
 * <ul>
 *   <li>Enable alphabetic property sorting for {@link RuneDataType} annotated classes.</li>
 *   <li>Support custom polymorphic type resolution for {@link RuneDataType} annotations.</li>
 *   <li>Provide custom name mapping for serialization and deserialization based on {@link RuneAttribute}.</li>
 *   <li>Handle property unwrapping for {@link RuneMetaType} annotations.</li>
 *   <li>Define property inclusion/exclusion rules for {@link RuneDataType} classes.</li>
 * </ul>
 *
 * <p>This class overrides several key methods in {@link JacksonAnnotationIntrospector}
 * to integrate these custom behaviors.
 *
 * <p>Key functionality includes:
 * <ul>
 *   <li>Polymorphic type resolution for {@link RuneDataType} using custom {@link StdTypeResolverBuilder}.</li>
 *   <li>Explicit handling of property ordering limitations documented in
 *       <a href="https://github.com/FasterXML/jackson-databind/issues/1670">jackson-databind issue 1670</a>.</li>
 *   <li>Integration with {@link RuneAttribute} to control property name mapping for
 *       serialization and deserialization.</li>
 *   <li>Support for unwrapping of properties with {@link RuneMetaType} using a no-operation
 *       {@link NameTransformer}.</li>
 * </ul>
 *
 * @see JacksonAnnotationIntrospector
 * @see StdTypeResolverBuilder
 * @see RuneDataType
 * @see RuneAttribute
 * @see RuneMetaType
 */
public class RuneJsonAnnotationIntrospector extends JacksonAnnotationIntrospector {
    private static final long serialVersionUID = 1L;
    private static final ArrayList<String> SERIALIZATION_PROPERTY_ORDER = Lists.newArrayList(META, REFERENCE, EXTERNAL_REFERENCE, SCOPED_REFERENCE, DATA);

    private final RuneEnumBuilderIntrospector runeEnumBuilderIntrospector;

    public RuneJsonAnnotationIntrospector() {
        runeEnumBuilderIntrospector = new RuneEnumBuilderIntrospector();
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
        if (t != null  && !isAnnotatedTypeMetaFields(ann)) {
            return JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CLASS,
                    JsonTypeInfo.As.EXISTING_PROPERTY,
                    RuneJsonConfig.MetaProperties.TYPE,
                    JsonTypeInfo.class,
                    true,
                    false);
        }
        return super.findPolymorphicTypeInfo(config, ann);
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

    @Override
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass enumType,
                                   Enum<?>[] enumValues, String[] names) {
        if (runeEnumBuilderIntrospector.isApplicable(enumType)) {
            runeEnumBuilderIntrospector.findEnumValues(enumType, enumValues, names);
            return names;
        }
        return super.findEnumValues(config, enumType, enumValues, names);
    }

    @Override
    public void findEnumAliases(MapperConfig<?> config, AnnotatedClass enumType,
                                Enum<?>[] enumValues, String[][] aliasList) {
        if (runeEnumBuilderIntrospector.isApplicable(enumType)) {
            runeEnumBuilderIntrospector.findEnumAliases(enumType, enumValues, aliasList);
        } else {
            super.findEnumAliases(config, enumType, enumValues, aliasList);
        }
    }

    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RuneDataType.class)) {
            return SERIALIZATION_PROPERTY_ORDER.toArray(new String[0]);
        }
        return super.findSerializationPropertyOrder(ac);
    }

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember a) {
        return a.getName().startsWith("add") || a.hasAnnotation(RuneIgnore.class) || super.hasIgnoreMarker(a);
    }

    private Set<String> getPropertyNames(AnnotatedClass acc, Predicate<AnnotatedMethod> filter) {
        return StreamSupport.stream(acc.memberMethods().spliterator(), false)
                .filter(filter)
                .map(RuneBeanUtil::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isAnnotatedTypeMetaFields(Annotated ann) {
        return ann.getType().getRawClass() == MetaFields.class;
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

}
