package com.regnosys.rosetta.common.util;

import java.io.Serializable;
import java.util.Objects;

public class Pair<L, R> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6124924390542411482L;

	private final L left;
	private final R right;

	private Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public static <L, R> Pair<L, R> of(L left, R right) {
		return new Pair<>(left, right);
	}

	public L left() {
		return left;
	}

	public R right() {
		return right;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Pair)) {
			return false;
		}
		Pair<?, ?> otherP = (Pair<?, ?>) other;
		return Objects.equals(left, otherP.left) && Objects.equals(right, otherP.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}
	
	@Override
	public String toString() {
		return "(" + Objects.toString(left, "null") + "," + Objects.toString(right, "null") + ")"; 
	}
}
