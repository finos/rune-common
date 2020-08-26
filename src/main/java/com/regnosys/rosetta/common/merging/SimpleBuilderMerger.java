package com.regnosys.rosetta.common.merging;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.merge.BuilderMerger;

import java.util.function.Consumer;

public class SimpleBuilderMerger implements BuilderMerger {

	@Override
	public <B extends RosettaModelObjectBuilder> boolean mergeRosetta(B left, B right, Consumer<B> setter) {
		if (left != null && right != null) {
			setter.accept(left);
			return true;
		}
		if (left == null) {
			setter.accept(right);
			return false;
		} else {
			return false;
		}
	}

	@Override
	public <T> void mergeBasic(T left, T right, Consumer<T> setter) {
		if (left != null && right != null && !left.equals(right)) {
			throw new IllegalArgumentException(
					String.format("Attempting to merge 2 different basic values [left=%s, right=%s, type=%s]",
							left, right, left.getClass().getSimpleName()));
		}
		if (left != null) {
			setter.accept(left);
		} else if (right != null) {
			setter.accept(right);
		}
	}
}
