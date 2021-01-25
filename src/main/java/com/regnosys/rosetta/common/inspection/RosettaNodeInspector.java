package com.regnosys.rosetta.common.inspection;

@Deprecated
public class RosettaNodeInspector<T> {

	public interface Visitor<T> {

		void onNode(Node<T> node);
	}

	public void inspect(Node<T> rootNode, Visitor<T> visitor) {
		inspect(rootNode, visitor, false);
	}

	public void inspect(Node<T> rootNode, Visitor<T> visitor, boolean childrenFirst) {
		if (!childrenFirst)
			visitor.onNode(rootNode);
		inspectChildren(rootNode, visitor, childrenFirst);
		if (childrenFirst)
			visitor.onNode(rootNode);
	}

	private void inspectChildren(Node<T> node, Visitor<T> visitor, boolean childrenFirst) {
		for (Node<T> childNode : node.getChildren()) {
			if (!node.isGuarded(childNode)) {
				if (!childrenFirst)
					visitor.onNode(childNode);

				if (childNode.inspect()) {
					inspectChildren(childNode, visitor, childrenFirst);
				}

				if (childrenFirst)
					visitor.onNode((childNode));
			}
		}
	}
}
