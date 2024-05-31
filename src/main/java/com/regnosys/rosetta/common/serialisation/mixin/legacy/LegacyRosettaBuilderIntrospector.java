package com.regnosys.rosetta.common.serialisation.mixin.legacy;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.google.common.collect.Sets;
import com.regnosys.rosetta.common.serialisation.BackwardsCompatibleAnnotationIntrospector;
import com.regnosys.rosetta.common.serialisation.BeanUtil;
import com.regnosys.rosetta.common.serialisation.RosettaSerialiserException;
import com.regnosys.rosetta.common.util.StringExtensions;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMeta;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LegacyRosettaBuilderIntrospector {

    public Optional<Class<?>> findPOJOBuilder(AnnotatedClass ac) {
        JavaType type = ac.getType();
        // [Ljava.lang.String  type is null!
        if (null != type) {
            Class<?> rawClass = type.getRawClass();

            if (RosettaModelObject.class.isAssignableFrom(rawClass)) {
                try {
                    String builderName = null;
                    if (rawClass.getName().endsWith("BuilderImpl")) builderName = rawClass.getName();
                    else if (rawClass.getName().endsWith("Builder")) builderName = rawClass.getName() + "Impl";
                    else if (rawClass.getName().endsWith("Impl"))
                        builderName = rawClass.getName().replaceAll("Impl$", "BuilderImpl");
                    else builderName = rawClass.getTypeName() + "$" + rawClass.getSimpleName() + "BuilderImpl";

                    return Optional.of(Class.forName(builderName, true, rawClass.getClassLoader()));

                } catch (ClassNotFoundException e) {
                    throw new RosettaSerialiserException("Could not find the builder class for " + rawClass, e);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<PropertyName> findNameForDeserialization(Annotated a) {
        if (a instanceof AnnotatedMethod) {
            AnnotatedMethod am = (AnnotatedMethod) a;
            if (am.getParameterCount() == 1) {
                if (am.getName().startsWith("set") && RosettaModelObject.class.isAssignableFrom(am.getDeclaringClass())) {
                    String firstLower = StringExtensions.toFirstLower(am.getName().substring(3));
                    if (firstLower.equals("key") && GlobalKeyFields.class.isAssignableFrom(am.getDeclaringClass())) {
                        firstLower = "location";
                    }
                    if (firstLower.equals("reference") && ReferenceWithMeta.class.isAssignableFrom(am.getDeclaringClass())) {
                        firstLower = "address";
                    }
                    return Optional.of(new PropertyName(firstLower));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<JsonIgnoreProperties.Value> findPropertyIgnorals(Annotated ac) {
        if (ac instanceof AnnotatedClass) {
            AnnotatedClass acc = (AnnotatedClass) ac;
            Set<String> names = null;
            if (RosettaModelObject.class.isAssignableFrom(ac.getRawType())) {
                names = StreamSupport.stream(acc.memberMethods().spliterator(), false)
                        .map(m -> BeanUtil.getPropertyName(m.getAnnotated()))
                        .filter(Objects::nonNull)
                        .filter(n -> n.startsWith("orCreate") || n.startsWith("type") || n.startsWith("valueType"))
                        .collect(Collectors.toSet());
            } else {
                names = Sets.newHashSet();
            }
            return Optional.of(JsonIgnoreProperties.Value.forIgnoredProperties(names).withAllowSetters());
        }
        if (ac instanceof AnnotatedMethod) {
            AnnotatedMethod am = (AnnotatedMethod) ac;
            String propertyName = BeanUtil.getPropertyName(am.getAnnotated());
            if (propertyName != null && propertyName.startsWith("orCreate")) {
                return Optional.of(JsonIgnoreProperties.Value.forIgnoredProperties(propertyName).withAllowSetters());
            }
        }
        return Optional.empty();
    }
}
