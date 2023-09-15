package com.regnosys.rosetta.common.serialisation.mixin;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;

import java.lang.reflect.Field;
import java.util.stream.Stream;

public class RosettaEnumBuilderIntrospector {

    public boolean isApplicable(Class<?> enumType) {
        return enumType.getAnnotation(RosettaEnum.class) != null;
    }

    public void findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .filter(f -> f.getAnnotation(RosettaEnumValue.class) != null)
                .forEach(f -> {
                    String value = f.getAnnotation(RosettaEnumValue.class).value();
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            names[i] = value;
                        }
                    }
                });
    }

    public void findEnumAliases(Class<?> enumType, Enum<?>[] enumValues, String[][] aliasList) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .filter(f -> f.getAnnotation(RosettaEnumValue.class) != null)
                .forEach(f -> {
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            aliasList[i] = new String[]{name};
                        }
                    }
                });
    }
}
