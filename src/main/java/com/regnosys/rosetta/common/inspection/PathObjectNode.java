package com.regnosys.rosetta.common.inspection;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.regnosys.rosetta.common.util.StreamUtils;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.ReflectUtils.*;

public class PathObjectNode implements Node<PathObject<Object>> {

    static Predicate<Node<PathObject<Object>>> IS_NULL = (node) -> node.get() == null;

    public static PathObjectNode root(Object object) {
        return new PathObjectNode(new PathObject<>(object.getClass().getSimpleName(), object));
    }

    private final PathObject<Object> pathObject;

    public PathObjectNode(PathObject<Object> pathObject) {
        this.pathObject = pathObject;
    }

    @Override
    public List<Node<PathObject<Object>>> getChildren() {
        Object object = pathObject.getObject();
        return methods(object.getClass()).stream().filter(StreamUtils.distinctByKey(m->m.getName()))
                .map(method -> mapToPathObjects(object, method))
                .flatMap(List::stream)
                .map(PathObjectNode::new)
                .collect(Collectors.toList());
    }

    @Override
    public PathObject<Object> get() {
        return pathObject;
    }

    @Override
    public boolean isGuarded(Node<PathObject<Object>> childNode) {
        return IS_NULL.test(childNode);
    }

    @Override
    public boolean inspect() {
        return RosettaModelObject.class.isInstance(this.pathObject.getObject()) ||
                RosettaModelObjectBuilder.class.isInstance(this.pathObject.getObject());
    }

    private List<PathObject<Object>> mapToPathObjects(Object object, Method method) {
        List<?> resultOfGetter = handleReturnTypes(invokeGetter(object, method));

        List<PathObject<Object>> children = new ArrayList<>();
        for (int i = 0; i < resultOfGetter.size(); i++) {
            Object o = resultOfGetter.get(i);
            children.add(returnsList(method) ?
                    new PathObject<>(pathObject, attrName(method), i, o) :
                    new PathObject<>(pathObject, attrName(method), o));
        }
        return children;
    }

    private List<?> handleReturnTypes(Object invoke) {
        if (invoke instanceof List) {
            return (List<?>) invoke;
        }
        return Collections.singletonList(invoke);
    }
}
