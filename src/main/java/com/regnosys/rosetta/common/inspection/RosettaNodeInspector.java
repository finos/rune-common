package com.regnosys.rosetta.common.inspection;

import java.util.List;
import java.util.function.BiPredicate;

public class RosettaNodeInspector<T> {

    public interface Node<T> {
        T get();

        boolean visited(Node<T> childNode);

        boolean inspect();

        List<Node<T>> getChildren();
    }

    public interface Visitor<T> {
        void onNode(Node<T> node);
    }

    private final BiPredicate<Node<T>, Node<T>> visitorGuard;

    public RosettaNodeInspector() {
        this((parent, child) -> !parent.visited(child));
    }

    public RosettaNodeInspector(BiPredicate<Node<T>, Node<T>> visitorGuard) {
        this.visitorGuard = visitorGuard;
    }

    public void inspect(Node<T> rootNode, Visitor<T> visitor, Visitor<T> rootVisitor) {
        rootVisitor.onNode(rootNode);
        inspect(rootNode, visitor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void inspect(Node<T> node, Visitor<T> visitor) {
        for(Node<T> childNode : node.getChildren()) {
            if (visitorGuard.test(node, childNode)) {
                visitor.onNode(childNode);

                if (childNode.inspect()) {
                    inspect(childNode, visitor);
                }
            }
        }
    }
}
