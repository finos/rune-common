package com.regnosys.rosetta.common.inspection;

/*-
 * ==============
 * Rosetta Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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
