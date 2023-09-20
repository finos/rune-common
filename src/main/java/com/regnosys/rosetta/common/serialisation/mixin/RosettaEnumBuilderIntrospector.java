package com.regnosys.rosetta.common.serialisation.mixin;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class RosettaEnumBuilderIntrospector {

    private final EnumNameFunc enumNameFunc;
    private final EnumAliasFunc enumAliasFunc;
    private final boolean supportNativeEnumValue;

    public RosettaEnumBuilderIntrospector(boolean supportNativeEnumValue) {
        this.supportNativeEnumValue = supportNativeEnumValue;
        if (supportNativeEnumValue) {
            this.enumNameFunc = (annotation, javaEnumName) -> annotation.value();
        } else {
            this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().equals("") ? annotation.displayName() : javaEnumName;
        }
        this.enumAliasFunc = (annotation, javaEnumName) -> !annotation.displayName().equals("") ?
                new String[]{javaEnumName, annotation.displayName(), annotation.value()}:
                new String[]{javaEnumName,  annotation.value()};
    }

    public boolean isApplicable(Class<?> enumType) {
        return enumType.getAnnotation(RosettaEnum.class) != null;
    }

    public void findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .filter(f -> f.getAnnotation(RosettaEnumValue.class) != null)
                .forEach(f -> {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            names[i] = enumNameFunc.apply(annotation, name);
                        }
                    }
                });
    }

    public void findEnumAliases(Class<?> enumType, Enum<?>[] enumValues, String[][] aliasList) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .filter(f -> f.getAnnotation(RosettaEnumValue.class) != null)
                .forEach(f -> {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            aliasList[i] = enumAliasFunc.apply(annotation, name);
                        }
                    }
                });
    }


    interface EnumNameFunc extends BiFunction<RosettaEnumValue, String, String> {

    }

    interface EnumAliasFunc extends BiFunction<RosettaEnumValue, String, String[]> {

    }
}
