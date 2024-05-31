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

import static com.regnosys.rosetta.common.inspection.ReflectUtils.attrName;
import static com.regnosys.rosetta.common.inspection.ReflectUtils.methods;
import static com.regnosys.rosetta.common.inspection.ReflectUtils.returnType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.rosetta.model.lib.RosettaModelObject;

public class PathTypeNode implements Node<PathObject<Class<?>>> {

    static Predicate<Node<PathObject<Class<?>>>> INSPECTED = (node) -> {
    	return node.get().getPathObjects().size()>15;
    	//simple depth limiting isn't perfect but the algorithm below doesn't allow for any searching in recursive data structures
    	//e.g. ContractualProduct->econonmicTerms->optionPayout->underlyer->contractualProduct
        /*List<Class<?>> path = node.get().getPathObjects();
        path.removeLast();
        return path.contains(node.get().getObject());*/
    };

    public static PathTypeNode root(Class<?> type) {
        return new PathTypeNode(new PathObject<>(type.getSimpleName(), type));
    }

    private final PathObject<Class<?>> pathType;

    public PathTypeNode(PathObject<Class<?>> pathType) {
        this.pathType = pathType;
    }

    @Override
    public List<Node<PathObject<Class<?>>>> getChildren() {
        return methods(pathType.getObject()).stream()
                .map(this::toPathObject)
                .map(PathTypeNode::new)
                .collect(Collectors.toList());
    }
    
    private PathObject<Class<?>> toPathObject(Method method) {
    	
    	Class<?> returnType = returnType(method);
		return new PathObject<>(pathType, attrName(method), returnType);
    }

    @Override
    public PathObject<Class<?>> get() {
        return pathType;
    }

    @Override
    public boolean isGuarded(Node<PathObject<Class<?>>> childNode) {
        return INSPECTED.test(childNode);
    }

    @Override
    public boolean inspect() {
        return RosettaModelObject.class.isAssignableFrom(this.pathType.getObject());
    }
}
