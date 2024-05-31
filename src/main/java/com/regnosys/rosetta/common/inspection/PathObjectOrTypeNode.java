package com.regnosys.rosetta.common.inspection;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

import com.rosetta.model.lib.RosettaModelObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.regnosys.rosetta.common.inspection.PathObjectOrTypeNode.ObjectAndType;

import static com.regnosys.rosetta.common.inspection.ReflectUtils.*;

public class PathObjectOrTypeNode implements Node<PathObject<ObjectAndType>> {

	static Predicate<Node<PathObject<ObjectAndType>>> IS_NULL = (node) -> node	.get()
																				.getObject()
																				.getObject() == null;

	static Predicate<Node<PathObject<ObjectAndType>>> MAX_DEPTH = (node) -> {
		return node	.get()
					.getPathObjects()
					.size() > 15;
		// simple depth limiting isn't perfect but the algorithm below doesn't allow for any searching in recursive data
		// structures
		// e.g. ContractualProduct->econonmicTerms->optionPayout->underlyer->contractualProduct
	};

	private final PathObject<ObjectAndType> pathObject;

	public static PathObjectOrTypeNode root(Object object, Class<?> type) {
		return new PathObjectOrTypeNode(new PathObject<>(type.getSimpleName(), new ObjectAndType(object, type)));
	}

	public PathObjectOrTypeNode(PathObject<ObjectAndType> pathObject) {
		this.pathObject = pathObject;
	}

	@Override
	public List<Node<PathObject<ObjectAndType>>> getChildren() {
		if (pathObject	.getObject()
						.getObject() != null) {
			Object object = pathObject	.getObject()
										.getObject();
			return methods(object.getClass())	.stream()
												.map(method -> mapToPathObjects(object, method))
												.flatMap(List::stream)
												.map(PathObjectOrTypeNode::new)
												.collect(Collectors.toList());
		} else
			return methods(pathObject	.getObject()
										.getType())	.stream()
													.map(this::toPathObject)
													.map(PathObjectOrTypeNode::new)
													.collect(Collectors.toList());
	}

	private PathObject<ObjectAndType> toPathObject(Method method) {

		Class<?> returnType = returnType(method);
		return new PathObject<>(pathObject, attrName(method), new ObjectAndType(null, returnType));
	}

	private List<PathObject<ObjectAndType>> mapToPathObjects(Object object, Method method) {
		List<?> resultOfGetter = handleReturnTypes(invokeGetter(object, method));

		List<PathObject<ObjectAndType>> children = new ArrayList<>();
		for (int i = 0; i < resultOfGetter.size(); i++) {
			Object o = resultOfGetter.get(i);
			ObjectAndType oc = null;
			if (o != null) {
				oc = new ObjectAndType(o, o.getClass());
			} else {
				Class<?> c = returnType(method);
				oc = new ObjectAndType(o, c);
			}
			children.add(returnsList(method) ? new PathObject<>(pathObject, attrName(method), i, oc) : new PathObject<>(pathObject, attrName(method), oc));
		}
		return children;
	}

	private List<?> handleReturnTypes(Object invoke) {
		if (invoke instanceof List) {
			return (List<?>) invoke;
		}
		return Collections.singletonList(invoke);
	}

	@Override
	public PathObject<ObjectAndType> get() {
		return pathObject;
	}

	@Override
	public boolean isGuarded(Node<PathObject<ObjectAndType>> childNode) {
		if (!IS_NULL.test(childNode))
			return false;
		return MAX_DEPTH.test(childNode);
	}

	@Override
	public boolean inspect() {
		return RosettaModelObject.class.isAssignableFrom(this.pathObject.getObject()
																		.getType());
	}

	public static class ObjectAndType {
		private final Object object;
		private final Class<?> clazz;

		public ObjectAndType(Object object, Class<?> clazz) {
			super();
			this.object = object;
			this.clazz = clazz;
		}

		public Object getObject() {
			return object;
		}

		public Class<?> getType() {
			return clazz;
		}

		@Override
		public String toString() {
			String o = object != null ? object.toString() : "null";
			o = o.substring(0, Math.min(o.length(), 15));
			return '"' + o + "\":" + clazz.getSimpleName();
		}
	}
}
