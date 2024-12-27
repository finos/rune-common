package poc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.rosetta.model.lib.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class MyRosettaJSONAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    private final EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector;

    private final RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector;

    public MyRosettaJSONAnnotationIntrospector(boolean supportRosettaEnumValue) {
        this(new EnumAsStringBuilderIntrospector(), new RosettaEnumBuilderIntrospector(supportRosettaEnumValue));
    }

    public MyRosettaJSONAnnotationIntrospector(EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector, RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector) {
        this.rosettaEnumBuilderIntrospector = rosettaEnumBuilderIntrospector;
        this.enumAsStringBuilderIntrospector = enumAsStringBuilderIntrospector;
    }


    @Override
    protected StdTypeResolverBuilder _constructStdTypeResolverBuilder() {
        return new MyStdTypeResolverBuilder();
    }

    @Override
    protected TypeResolverBuilder<?> _constructStdTypeResolverBuilder(MapperConfig<?> config,
                                                                      JsonTypeInfo.Value typeInfo, JavaType baseType) {
        return new MyStdTypeResolverBuilder(typeInfo);
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

    @Override
    protected StdTypeResolverBuilder _constructNoTypeResolverBuilder() {
        return MyStdTypeResolverBuilder.noTypeInfoBuilder();
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
        } else if (a.hasAnnotation(RosettaAttribute.class)) {
            return new PropertyName(a.getAnnotation(RosettaAttribute.class).value());
        }
        return super.findNameForSerialization(a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        if (a.hasAnnotation(RuneAttribute.class)) {
            return new PropertyName(a.getAnnotation(RuneAttribute.class).value());
        } else if (a.hasAnnotation(RosettaAttribute.class)) {
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
        RuneMetaType ann = _findAnnotation(member, RuneMetaType.class);
        // if not enabled, just means annotation is not enabled; not necessarily
        // that unwrapping should not be done (relevant when using chained introspectors)
        if (ann == null) {
            return super.findUnwrappingNameTransformer(member);
        }
        return  NameTransformer.NOP;
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
                .map(m -> BeanUtil.getPropertyName(m.getAnnotated()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    static class RosettaEnumBuilderIntrospector {

        private final EnumNameFunc enumNameFunc;
        private final EnumAliasFunc enumAliasFunc;

        public RosettaEnumBuilderIntrospector(boolean supportRosettaEnumValue) {
            if (supportRosettaEnumValue) {
                this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ? annotation.displayName() : annotation.value();
            } else {
                this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ? annotation.displayName() : javaEnumName;
            }
            this.enumAliasFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ?
                    new String[]{javaEnumName, annotation.displayName(), annotation.value()} :
                    new String[]{javaEnumName, annotation.value()};
        }

        public boolean isApplicable(AnnotatedClass enumType) {
            return enumType.getAnnotation(RosettaEnum.class) != null;
        }

        public void findEnumValues(AnnotatedClass enumType, Enum<?>[] enumValues, String[] names) {
            for (AnnotatedField f : enumType.fields()) {
                if (f.hasAnnotation(RosettaEnumValue.class)) {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            names[i] = enumNameFunc.apply(annotation, name);
                            break;
                        }
                    }
                }
            }
        }

        public void findEnumAliases(AnnotatedClass enumType, Enum<?>[] enumValues, String[][] aliasList) {
            for (AnnotatedField f : enumType.fields()) {
                if (f.hasAnnotation(RosettaEnumValue.class)) {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            aliasList[i] = enumAliasFunc.apply(annotation, name);
                            break;
                        }
                    }
                }
            }
        }


        interface EnumNameFunc extends BiFunction<RosettaEnumValue, String, String> {

        }

        interface EnumAliasFunc extends BiFunction<RosettaEnumValue, String, String[]> {

        }
    }

    static class BeanUtil {

        public static String getPropertyName(Method method) {
            String methodName = method.getName();
            String rawPropertyName = getSubstringIfPrefixMatches(methodName, "get");
            if (rawPropertyName == null) {
                rawPropertyName = getSubstringIfPrefixMatches(methodName, "set");
            }

            if (rawPropertyName == null) {
                rawPropertyName = getSubstringIfPrefixMatches(methodName, "is");
            }

            if (rawPropertyName == null) {
                rawPropertyName = getSubstringIfPrefixMatches(methodName, "add");
            }

            return toLowerCamelCase(rawPropertyName);
        }

        public static String toLowerCamelCase(String string) {
            if (string == null) {
                return null;
            } else if (string.isEmpty()) {
                return string;
            } else if (string.length() > 1 && Character.isUpperCase(string.charAt(1)) && Character.isUpperCase(string.charAt(0))) {
                return string;
            } else {
                char[] chars = string.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                return new String(chars);
            }
        }

        private static String getSubstringIfPrefixMatches(String wholeString, String prefix) {
            return wholeString.startsWith(prefix) ? wholeString.substring(prefix.length()) : null;
        }
    }

    static public class EnumAsStringBuilderIntrospector {

        public void findEnumValues(AnnotatedClass enumType, Enum<?>[] enumValues, String[] names) {
            for (AnnotatedField f : enumType.fields()) {
                final String name = f.getName();
                for (int i = 0, end = enumValues.length; i < end; ++i) {
                    if (name.equals(enumValues[i].name())) {
                        names[i] = enumValues[i].toString();
                    }
                }
            }
        }
    }
}
