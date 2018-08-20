package com.regnosys.rosetta.common.inspection;

import com.rosetta.model.lib.RosettaModelObject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.RosettaReflectionsUtil.getAllPublicNoArgGetters;
import static com.regnosys.rosetta.common.inspection.RosettaNodeInspector.Node;
import static com.regnosys.rosetta.common.inspection.RosettaReflectionsUtil.getReturnType;

public class PathTypeNode implements Node<PathObject<Class<?>>> {

    public static PathTypeNode root(Class<?> type) {
        return new PathTypeNode(new PathObject<>(type.getSimpleName(), type));
    }

    private final PathObject<Class<?>> pathType;

    public PathTypeNode(PathObject<Class<?>> pathType) {
        this.pathType = pathType;
    }

    @Override
    public List<Node<PathObject<Class<?>>>> getChildren() {
        return getAllPublicNoArgGetters(pathType.getObject()).stream()
                .map(method -> new PathObject<>(pathType, attrName(method), getReturnType(method)))
                .map(PathTypeNode::new)
                .collect(Collectors.toList());
    }

    @Override
    public PathObject<Class<?>> get() {
        return pathType;
    }

    @Override
    public boolean test(Node<PathObject<Class<?>>> childNode) {
        return pathType.getPathObjects().contains(childNode.get().getObject());
    }

    @Override
    public boolean inspect() {
        return RosettaModelObject.class.isAssignableFrom(this.pathType.getObject());
    }

    private String attrName(Method method) {
        String attrName = method.getName().replace("get", "");
        return Character.toLowerCase(attrName.charAt(0)) + attrName.substring(1);
    }
}