package com.regnosys.rosetta.common.merging;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.merge.BuilderMerger;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleBuilderMerger implements BuilderMerger {

	@Override
	public <B extends RosettaModelObjectBuilder> void mergeRosetta(B left, B right, Supplier<B> getOrCreate, Consumer<B> setter) {
		if (left != null && right != null) {
			getOrCreate.get().merge(left, right, this);
		} else if (left != null) {
			setter.accept(left);
		} else if (right != null) {
			setter.accept(right);
		}
	}

	@Override
	public <B extends RosettaModelObjectBuilder> void mergeRosetta(List<B> left, List<B> right, Function<Integer, B> getOrCreate, Consumer<B> add) {
		if (!isNullOrEmpty(left) && !isNullOrEmpty(right)) {
			Iterator<B> leftIterator = left.iterator();
			Iterator<B> rightIterator = right.iterator();
			int index = 0;
			while (leftIterator.hasNext() && rightIterator.hasNext()) {
				getOrCreate.apply(index++).merge(leftIterator.next(), rightIterator.next(), this);
			}
		} else if (!isNullOrEmpty(left)) {
			left.forEach(add);
		} else if (!isNullOrEmpty(right)) {
			right.forEach(add);
		}
	}

	@Override
	public <T> void mergeBasic(T left, T right, Consumer<T> setter) {
		if (left != null && right != null && !left.equals(right)) {
			throw new IllegalArgumentException(
					String.format("Attempting to merge 2 different basic values [left=%s, right=%s, type=%s]",
							left, right, left.getClass().getSimpleName()));
		} else if (left != null) {
			setter.accept(left);
		} else if (right != null) {
			setter.accept(right);
		}
	}

	private boolean isNullOrEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}
}
