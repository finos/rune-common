package com.regnosys.rosetta.common.inspection;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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
 * ==============
 */

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.regnosys.rosetta.common.util.StringExtensions;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;

import com.rosetta.model.lib.path.RosettaPath;
import org.reflections.ReflectionUtils;

import javax.lang.model.type.ExecutableType;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Deprecated
public class ReflectUtils {

	public static Class<?> returnType(Method method) {
		if (method	.getReturnType()
					.equals(List.class)) {
			return getGenericType(method);
		} else {
			return method.getReturnType();
		}
	}

	public static Class<?> getGenericType(Method method) {
		ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
		Type type = genericReturnType.getActualTypeArguments()[0];
		if (type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) type).getRawType();
			return (Class<?>) rawType;
		}
		return (Class<?>) type;
	}

	public static boolean returnsList(Method method) {
		return method	.getReturnType()
						.equals(List.class);
	}

	public static Method getMethod(Class<?> originatingClass, String getter) throws NoSuchMethodException {
		return originatingClass.getMethod(getter);
	}

	@SuppressWarnings("unchecked")
	public static Class<?> genericType(Class<? extends RosettaModelObjectBuilder> clazz, String attributeName) {
		Class<?> type = getAttributeType(clazz, attributeName);

		if (List.class.isAssignableFrom(type)) {
			Set<Field> fields = ReflectionUtils.getAllFields(clazz, (field) -> field.getName()
																					.equals(attributeName));
			Field field = Iterables.getOnlyElement(fields);
			ParameterizedType genericReturnType = (ParameterizedType) field.getGenericType();
			Type genericType = genericReturnType.getActualTypeArguments()[0];
			if (genericType instanceof ParameterizedType) {
				genericType = ((ParameterizedType) genericType).getRawType();
			}
			return (Class<?>) genericType;
		} else {
			return type;
		}
	}

	@SuppressWarnings("unchecked")
	public static Class<?> ultimateGenericType(Class<? extends RosettaModelObjectBuilder> clazz,
			String attributeName) {

		Set<Field> fields = ReflectionUtils.getAllFields(clazz, (field) -> field.getName()
																				.equals(attributeName));
		Field field = Iterables.getOnlyElement(fields);
		return getFinalType(field.getGenericType());

	}

	private static Class<?> getFinalType(Type type) {
		if (type instanceof ParameterizedType) {
			return getFinalType(((ParameterizedType) type).getActualTypeArguments()[0]);
		}
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		return null;
	}

	public static String attrName(Method method) {
		return StringExtensions.toFirstLower(method	.getName()
													.replace("get", ""));
	}

	@SuppressWarnings("unchecked")
	public static Set<Method> methods(Class<?> clazz) {
		return ReflectionUtils.getAllMethods(clazz,
				ReflectUtils::isPublic,
				ReflectUtils::isGetter,
				ReflectUtils::isNotCreate,
				ReflectUtils::hasNoArgs);
	}

	@SuppressWarnings("unchecked")
	public static Set<Method> methods(Class<?> clazz, Predicate<? super Method> methodFilter) {
		return ReflectionUtils.getAllMethods(clazz,
				ReflectUtils::isPublic,
				ReflectUtils::isGetter,
				ReflectUtils::isNotCreate,
				ReflectUtils::hasNoArgs,
				methodFilter);
	}

	private static boolean isPublic(Method m) {
		return Modifier.isPublic(m.getModifiers());
	}

	private static boolean isGetter(Method m) {
		return m.getName()
				.startsWith("get");
	}

	private static boolean isNotCreate(Method m) {
		return !m	.getName()
					.startsWith("getOrCreate");
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

	public static Method setter(Class<? extends RosettaModelObjectBuilder> clazz, String attributeName, Class<?> attributeType)
			throws NoSuchMethodException, NoSuchFieldException {
		Class<?> type = getAttributeType(clazz, attributeName);
		String setterPrefix = List.class.isAssignableFrom(type) ? "add" : "set";
		return clazz.getMethod(setterPrefix + StringExtensions.toFirstUpper(attributeName), attributeType);
	}

	/**
	 * Given a HierarchicalPath, this method reflectively gets the value from the rosetta instance.
	 * 
	 * @param object
	 * @param hierarchicalPath
	 * @return
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static Optional<Object> valueOf(Object object, RosettaPath hierarchicalPath)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Object current = object;

		for (RosettaPath.Element element : hierarchicalPath.allElements()) {
			Method method = current.getClass().getMethod(getterName(element.getPath()));
			Object invoke = method.invoke(current);
			if (element.getIndex().isPresent()) {
				List<?> listType = (List<?>) invoke;

				invoke = listType.get(element.getIndex().getAsInt());
			}
			if (invoke == null) return Optional.empty();
			current = invoke;
		}
		return Optional.ofNullable(current);
	}

	@SuppressWarnings("unchecked")
	public static void setChildBuilder(RosettaModelObjectBuilder parentBuilder, RosettaModelObjectBuilder childBuilder, String attributeName, int index)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, NoSuchMethodException {
		Field field = Iterables.getOnlyElement(ReflectionUtils.getAllFields(parentBuilder.getClass(), ReflectionUtils.withName(attributeName)));
		field.setAccessible(true);

		if (field	.getType()
					.equals(List.class)) {
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
		return (method) -> method	.getName()
									.equals(name);
	}

	private static String getterName(String attributeName) {
		return "get" + StringExtensions.toFirstUpper(attributeName);
	}

	public static Method getter(Class<? extends RosettaModelObject> clazz, String attributeName) {
		Set<Method> methods = methods(clazz, (method) -> method	.getName()
																.equals(getterName(attributeName)));
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
