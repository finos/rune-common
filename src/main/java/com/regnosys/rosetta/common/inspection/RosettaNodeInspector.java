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

    public void inspect(Node<T> rootNode, Visitor<T> visitor) {
        inspect(rootNode, visitor, visitor);
    }

    public void inspect(Node<T> rootNode, Visitor<T> visitor, Visitor<T> rootVisitor) {
        rootVisitor.onNode(rootNode);
        inspectChildren(rootNode, visitor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void inspectChildren(Node<T> node, Visitor<T> visitor) {
        for(Node<T> childNode : node.getChildren()) {
            if (!node.isGuarded(childNode)) {
                visitor.onNode(childNode);

                if (childNode.inspect()) {
                    inspectChildren(childNode, visitor);
                }
            }
        }
    }
}
