package com.regnosys.rosetta.common.inspection;

public class RosettaNodeInspector<T> {

    public interface Visitor<T> {

        default void onNodeGuard(Node<T> node) {
            if (node.inspect()) {
                onNode(node);
            }
        }

        void onNode(Node<T> node);
    }

    public void inspect(Node<T> rootNode, Visitor<T> visitor) {
        inspect(rootNode, visitor, false);
    }

    public void inspect(Node<T> rootNode, Visitor<T> visitor, boolean childrenFirst) {
        if (!childrenFirst) visitor.onNodeGuard(rootNode);
        inspectChildren(rootNode, visitor, childrenFirst);
        if (childrenFirst) visitor.onNodeGuard(rootNode);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void inspectChildren(Node<T> node, Visitor<T> visitor, boolean childrenFirst) {
        for (Node<T> childNode : node.getChildren()) {
            if (!node.isGuarded(childNode)) {
                if (!childrenFirst) visitor.onNodeGuard(childNode);

                if (childNode.inspect()) {
                    inspectChildren(childNode, visitor, childrenFirst);
                }

                if (childrenFirst) visitor.onNodeGuard((childNode));
            }
        }
    }
}
