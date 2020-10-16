package com.regnosys.rosetta.common.merging;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.merge.BuilderMerger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.rosetta.util.CollectionUtils.*;

public class SimpleUnmerger implements BuilderMerger {

	public static <B extends RosettaModelObjectBuilder> B unmerge(B instance, B template) {
		return instance.merge(template, new SimpleUnmerger()).prune();
	}

	@Override
	public <B extends RosettaModelObjectBuilder> void mergeRosetta(B left, B right, Consumer<B> setter) {
		if (left != null && right != null) {
			left.merge(right, this);
		}
	}

	@Override
	public <B extends RosettaModelObjectBuilder> void mergeRosetta(List<B> left, List<B> right, Function<Integer, B> getOrCreate, Consumer<B> add) {
		AtomicInteger index = new AtomicInteger();
		for (Iterator<B> l = copy(left).iterator(), r = copy(right).iterator(); l.hasNext() || r.hasNext(); index.getAndIncrement()) {
			mergeRosetta(nextOrGet(l, () -> getOrCreate.apply(index.get())), nextOrNull(r), add);
		}
	}

	@Override
	public <T> void mergeBasic(T left, T right, Consumer<T> setter) {
		if (left != null && right != null) {
			setter.accept(null);
		}
	}

	@Override
	public <T> void mergeBasic(List<T> left, List<T> right, Consumer<T> add) {
		emptyIfNull(left).removeAll(emptyIfNull(right));
	}
}
