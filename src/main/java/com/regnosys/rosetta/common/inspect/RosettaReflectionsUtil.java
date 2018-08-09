package com.regnosys.rosetta.common.inspect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import static org.reflections.ReflectionUtils.getAllMethods;

public class RosettaReflectionsUtil {

    @SuppressWarnings({"unchecked"})
    public static Set<Method> getAllPublicNoArgGetters(Class<?> type) {
        return getAllMethods(type,
                RosettaReflectionsUtil::isPublic,
                RosettaReflectionsUtil::isGetter,
                RosettaReflectionsUtil::hasNoArgs);
    }

    private static boolean isPublic(Method m) {
        return Modifier.isPublic(m.getModifiers());
    }

    private static boolean isGetter(Method m) {
        return m.getName().startsWith("get");
    }

    private static boolean hasNoArgs(Method m) {
        return m.getParameterTypes().length == 0;
    }

    public static Class<?> getReturnType(Method method) {
        Class<?> returnClass = method.getReturnType();

        if (List.class.isAssignableFrom(returnClass)) {
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) returnType;
                Type type = paramType.getActualTypeArguments()[0];
                return (Class<?>) type;
            }
        }
        return returnClass;
    }
}
