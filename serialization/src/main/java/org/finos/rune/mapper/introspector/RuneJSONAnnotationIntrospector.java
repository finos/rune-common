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
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
 * <p>This implementation is part of the Rune Common library and adheres to the Apache License 2.0.
 *
 * @see JacksonAnnotationIntrospector
 * @see StdTypeResolverBuilder
 * @see RuneDataType
 * @see RuneAttribute
 * @see RuneMetaType
 */
public class RuneJSONAnnotationIntrospector extends JacksonAnnotationIntrospector {
    private static final long serialVersionUID = 1L;

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

    /**
     * Enables alphabetic sorting for properties of classes annotated with {@link RuneDataType}.
     *
     * <p>Currently, only alphabetic ordering of properties is supported. Ideally, explicit
     * ordering could be achieved by overriding the parent method:
     * <pre>{@code String[] findSerializationPropertyOrder(AnnotatedClass ac)}</pre>.
     *
     * <p>However, due to a limitation in Jackson, the property ordering logic does not work
     * in conjunction with unwrapping logic. This issue is documented in the following ticket:
     * <a href="https://github.com/FasterXML/jackson-databind/issues/1670">jackson-databind issue 1670</a>.
     */
    @Override
    public Boolean findSerializationSortAlphabetically(Annotated ann) {
        if (ann.hasAnnotation(RuneDataType.class)) {
            return Boolean.TRUE;
        }
        return super.findSerializationSortAlphabetically(ann);
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
