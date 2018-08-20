package com.regnosys.rosetta.common.inspection;

import java.lang.reflect.*;
import java.util.Collections;
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

    public static Object invokeGetter(Object o, Method method) {
        try {
            return method.invoke(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Tried to call method %s on %s but was not allowed." +
                    " Rosetta code generation assumptions have been broken.", method.getName(), o.getClass()), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(String.format("Something went wrong when trying to call method %s on %s.",
                    method.getName(), o.getClass()), e);
        }
    }

    public static List<?> handleReturnTypes(Object invoke) {
        if (invoke instanceof List) {
            return (List<?>) invoke;
        }
        if (invoke instanceof Enum) {
            return Collections.emptyList();
        }
        return Collections.singletonList(invoke);
    }
}
