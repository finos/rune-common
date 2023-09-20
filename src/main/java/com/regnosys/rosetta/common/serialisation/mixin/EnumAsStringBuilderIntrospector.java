package com.regnosys.rosetta.common.serialisation.mixin;

import java.lang.reflect.Field;
import java.util.stream.Stream;

public class EnumAsStringBuilderIntrospector {

    public void findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .forEach(f -> {
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            names[i] = enumValues[i].toString();
                        }
                    }
                });
    }
}
