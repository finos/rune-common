package poc;

import annotations.RuneUnwrapped;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class MyRosettaJSONAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    private final PocMain.EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector;

    private final PocMain.RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector;

    public MyRosettaJSONAnnotationIntrospector(boolean supportRosettaEnumValue) {
        this(new PocMain.EnumAsStringBuilderIntrospector(), new PocMain.RosettaEnumBuilderIntrospector(supportRosettaEnumValue));
    }

    public MyRosettaJSONAnnotationIntrospector(PocMain.EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector, PocMain.RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector) {
        this.rosettaEnumBuilderIntrospector = rosettaEnumBuilderIntrospector;
        this.enumAsStringBuilderIntrospector = enumAsStringBuilderIntrospector;
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            return ac.getAnnotation(RosettaDataType.class).builder();
        }
        return super.findPOJOBuilder(ac);
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
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated ann) {
        return findPropertyIgnorals(ann);
    }

    @Override
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
        RuneUnwrapped ann = _findAnnotation(member, RuneUnwrapped.class);
        // if not enabled, just means annotation is not enabled; not necessarily
        // that unwrapping should not be done (relevant when using chained introspectors)
        if (ann == null) {
            return super.findUnwrappingNameTransformer(member);
        }
        return new MetaNameTransformer();
    }

    @Deprecated
    @Override
    public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated ac) {
        if (ac instanceof AnnotatedClass && ac.hasAnnotation(RosettaDataType.class)) {
            AnnotatedClass acc = (AnnotatedClass) ac;
            Set<String> includes = getPropertyNames(acc, x -> x.hasAnnotation(RosettaAttribute.class));
            Set<String> ignored = getPropertyNames(acc, x -> !x.hasAnnotation(RosettaAttribute.class));
            ignored.removeAll(includes);
            return JsonIgnoreProperties.Value.forIgnoredProperties(ignored).withAllowSetters();
        }

        return JsonIgnoreProperties.Value.empty();
    }

    private static Set<String> getPropertyNames(AnnotatedClass acc, Predicate<AnnotatedMethod> filter) {
        return StreamSupport.stream(acc.memberMethods().spliterator(), false)
                .filter(filter)
                .map(m -> PocMain.BeanUtil.getPropertyName(m.getAnnotated()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    static class MetaNameTransformer extends NameTransformer {
        @Override
        public String transform(String input) {
            switch (input) {
                case "globalKey": return "@key";
                case "externalKey": return "@key:external";
                default: return input;
            }
        }

        @Override
        public String reverse(String transformed) {
            switch (transformed) {
                case "@key": return "globalKey";
                case "@key:external": return "externalKey";
                default: return transformed;
            }
        }
    }
}
