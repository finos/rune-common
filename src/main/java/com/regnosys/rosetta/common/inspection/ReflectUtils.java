package com.regnosys.rosetta.common.inspection;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.regnosys.rosetta.common.util.StringExtensions;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.reflections.ReflectionUtils;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ReflectUtils {


    public static Class<?> returnType(Method method) {
        if (method.getReturnType().equals(List.class)) {
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            Type type = genericReturnType.getActualTypeArguments()[0];
            return (Class<?>) type;
        } else {
            return method.getReturnType();
        }
    }

    public static boolean returnsList(Method method) {
        return method.getReturnType().equals(List.class);
    }

    public static Method getMethod(Class<? extends RosettaModelObject> originatingClass, String getter) throws NoSuchMethodException {
        return originatingClass.getMethod(getter);
    }

    @SuppressWarnings("unchecked")
    public static Class<?> genericType(Class<? extends RosettaModelObjectBuilder> clazz, String attributeName) {
        Class<?> type = getAttributeType(clazz, attributeName);

        if (List.class.isAssignableFrom(type)) {
            Set<Field> fields = ReflectionUtils.getAllFields(clazz, (field) -> field.getName().equals(attributeName));
            Field field = Iterables.getOnlyElement(fields);
            ParameterizedType genericReturnType = (ParameterizedType) field.getGenericType();
            Type genericType = genericReturnType.getActualTypeArguments()[0];
            return (Class<?>) genericType;
        } else {
            return type;
        }
    }

    public static String attrName(Method method) {
        return StringExtensions.toFirstLower(method.getName().replace("get", ""));
    }

    @SuppressWarnings("unchecked")
    public static Set<Method> methods(Class<?> clazz) {
        return ReflectionUtils.getAllMethods(clazz,
                ReflectUtils::isPublic,
                ReflectUtils::isGetter,
                ReflectUtils::hasNoArgs);
    }

    @SuppressWarnings("unchecked")
    public static Set<Method> methods(Class<?> clazz, Predicate<? super Method> methodFilter) {
        return ReflectionUtils.getAllMethods(clazz,
                ReflectUtils::isPublic,
                ReflectUtils::isGetter,
                ReflectUtils::hasNoArgs,
                methodFilter);
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

    public static boolean isRosettaModelObject(Class<?> returnType) {
        return RosettaModelObject.class.isAssignableFrom(returnType);
    }

    @SuppressWarnings("unchecked")
    public static Optional<Class<? extends RosettaModelObject>> rosettaReturnType(Method method) {
        Class<?> returnType = returnType(method);
        if (isRosettaModelObject(returnType)) {
            return Optional.of((Class<? extends RosettaModelObject>) returnType);
        }
        return Optional.empty();
    }

    public static Method setter(Class<? extends RosettaModelObjectBuilder> clazz, String attributeName, Class<?> attributeType) throws NoSuchMethodException, NoSuchFieldException {
        Class<?> type = getAttributeType(clazz, attributeName);
        String setterPrefix = List.class.isAssignableFrom(type) ? "add" : "set";
        return clazz.getMethod(setterPrefix + StringExtensions.toFirstUpper(attributeName), attributeType);
    }

    @SuppressWarnings("unchecked")
    public static void setChildBuilder(RosettaModelObjectBuilder parentBuilder, RosettaModelObjectBuilder childBuilder, String attributeName, int index) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, NoSuchMethodException {
        Field field = Iterables.getOnlyElement(ReflectionUtils.getAllFields(parentBuilder.getClass(), ReflectionUtils.withName(attributeName)));
        field.setAccessible(true);

        if (field.getType().equals(List.class)) {
            if (field.get(parentBuilder) == null) {
                field.set(parentBuilder, new ArrayList<>());
            }
            List<RosettaModelObjectBuilder> list = (List<RosettaModelObjectBuilder>) field.get(parentBuilder);

            for (int i = 0; i < index + 1; i++) {
                if (list.size() <= i) {
                    list.add(null);
                }
            }
            list.set(index, childBuilder);

        } else {
            field.set(parentBuilder, childBuilder);
        }
    }
    private static Class<?> getAttributeType(Class<? extends RosettaModelObjectBuilder> clazz, String attributeName) {
        Set<Method> methods = methods(clazz, methodNameFilter(getterName(attributeName)));
        Method method = Iterables.getOnlyElement(methods);
        return method.getReturnType();
    }

    private static Predicate<Method> methodNameFilter(String name) {
        return (method) -> method.getName().equals(name);
    }

    private static String getterName(String attributeName) {
        return "get" + StringExtensions.toFirstUpper(attributeName);
    }


    public static Method getter(Class<? extends RosettaModelObjectBuilder> clazz, String attributeName) {
        Set<Method> methods = methods(clazz, (method) -> method.getName().equals(getterName(attributeName)));
        return Iterables.getOnlyElement(methods);
    }

    public static Object invokeGetter(Object o, Method method) {
        try {
            return method.invoke(o);
        } catch (IllegalAccessException e) {
            throw new InspectorException(String.format("Tried to call method %s on %s but was not allowed." +
                    " Rosetta code generation assumptions have been broken.", method.getName(), o.getClass()), e);
        } catch (InvocationTargetException e) {
            throw new InspectorException(String.format("Something went wrong when trying to call method %s on %s.",
                    method.getName(), o.getClass()), e);
        }
    }
}
