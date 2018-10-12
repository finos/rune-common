package com.regnosys.rosetta.common.inspection;

import com.rosetta.model.lib.RosettaModelObject;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.ReflectUtils.*;
import static com.regnosys.rosetta.common.inspection.RosettaNodeInspector.Node;

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
                .map(method -> new PathObject<>(pathType, attrName(method), returnType(method)))
                .map(PathTypeNode::new)
                .collect(Collectors.toList());
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