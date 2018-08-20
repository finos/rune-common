package com.regnosys.rosetta.common.inspection;

import java.util.List;

public class RosettaNodeInspector<T> {

    public interface Node<T> {
        T get();

        List<Node<T>> getChildren();

        boolean isGuarded(Node<T> node);

        boolean inspect();
    }

    public interface Visitor<T> {
        void onNode(Node<T> node);
    }

    public void inspect(Node<T> rootNode, Visitor<T> visitor, Visitor<T> rootVisitor) {
        rootVisitor.onNode(rootNode);
        inspect(rootNode, visitor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void inspect(Node<T> node, Visitor<T> visitor) {
        for(Node<T> childNode : node.getChildren()) {
            if (!node.isGuarded(childNode)) {
                visitor.onNode(childNode);

                if (childNode.inspect()) {
                    inspect(childNode, visitor);
                }
            }
        }
    }
}
