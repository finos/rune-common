package com.regnosys.rosetta.common.inspect;

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

    public interface RootVisitor<T> {
        RootVisitor NO_OP_ROOT_VISITOR = (node -> {});

        void onNode(Node<T> node);
    }

    private final BiPredicate<Node<T>, Node<T>> visitorGuard;

    public RosettaNodeInspector() {
        this((parent, child) -> !parent.visited(child));
    }

    public RosettaNodeInspector(BiPredicate<Node<T>, Node<T>> visitorGuard) {
        this.visitorGuard = visitorGuard;
    }

    public void inspect(Node<T> rootNode, Visitor<T> visitor, RootVisitor<T> rootVisitor) {
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
