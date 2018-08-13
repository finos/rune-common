package com.regnosys.rosetta.common.inspection;

import com.rosetta.model.lib.RosettaModelObject;

import java.util.List;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.RosettaReflectionsUtil.getAllPublicNoArgGetters;
import static com.regnosys.rosetta.common.inspection.RosettaNodeInspector.Node;

public class PathTypeNode implements Node<PathType> {

    private final PathType pathType;

    public PathTypeNode(PathType pathType) {
        this.pathType = pathType;
    }

    @Override
    public List<Node<PathType>> getChildren() {
        return getAllPublicNoArgGetters(pathType.getType()).stream()
                .map(method -> new PathType(pathType, method))
                .map(PathTypeNode::new)
                .collect(Collectors.toList());
    }

    @Override
    public PathType get() {
        return pathType;
    }

    @Override
    public boolean visited(Node<PathType> childNode) {
        return pathType.getPathTypes().contains(childNode.get().getType());
    }

    @Override
    public boolean inspect() {
        return RosettaModelObject.class.isAssignableFrom(this.pathType.getType());
    }
}