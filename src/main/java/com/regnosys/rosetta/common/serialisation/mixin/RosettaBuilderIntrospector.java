package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.regnosys.rosetta.common.serialisation.BackwardsCompatibleAnnotationIntrospector;
import com.regnosys.rosetta.common.serialisation.BeanUtil;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyRosettaBuilderIntrospector;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaClass;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RosettaBuilderIntrospector extends JacksonAnnotationIntrospector implements BackwardsCompatibleAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    private final LegacyRosettaBuilderIntrospector legacyRosettaBuilderIntrospector = new LegacyRosettaBuilderIntrospector();

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaClass.class)) {
            return ac.getAnnotation(RosettaClass.class).builder();
        }
        return legacyRosettaBuilderIntrospector.findPOJOBuilder(ac)
                .orElse(super.findPOJOBuilder(ac));
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
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated ann) {
        return findPropertyIgnorals(ann);
    }

    @Deprecated
    @Override
    public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated ac) {
        if (ac instanceof AnnotatedClass && ac.hasAnnotation(RosettaClass.class)) {
            AnnotatedClass acc = (AnnotatedClass) ac;
            Set<String> includes = getPropertyNames(acc, x -> x.hasAnnotation(RosettaAttribute.class));
            Set<String> ignored = getPropertyNames(acc, x -> !x.hasAnnotation(RosettaAttribute.class));
            ignored.removeAll(includes);
            return JsonIgnoreProperties.Value.forIgnoredProperties(ignored).withAllowSetters();
        }

        return legacyRosettaBuilderIntrospector.findPropertyIgnorals(ac)
                .orElse(JsonIgnoreProperties.Value.empty());

    }

    private static Set<String> getPropertyNames(AnnotatedClass acc, Predicate<AnnotatedMethod> filter) {
        return StreamSupport.stream(acc.memberMethods().spliterator(), false)
                .filter(filter)
                .map(m -> BeanUtil.getPropertyName(m.getAnnotated()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }
}
