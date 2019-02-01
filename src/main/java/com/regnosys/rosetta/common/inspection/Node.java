package com.regnosys.rosetta.common.inspection;

import java.util.List;

public interface Node<T> {
	T get();

	List<Node<T>> getChildren();

	boolean isGuarded(Node<T> node);

	boolean inspect();
}
