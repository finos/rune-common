package com.regnosys.rosetta.common.merging;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.rosetta.util.CollectionUtils.*;
import static java.util.Optional.ofNullable;

public class SimpleMerger implements BuilderMerger {

	private final Consumer<RosettaModelObjectBuilder> postProcessor;

	public SimpleMerger() {
		this(null);
	}

	public SimpleMerger(Consumer<RosettaModelObjectBuilder> postProcessor) {
		this.postProcessor = postProcessor;
	}

	@Override
	public <B extends RosettaModelObjectBuilder> B run(B left, B right) {
		B merged = left.merge(right, this);
		Optional.ofNullable(postProcessor).ifPresent(p -> p.accept(merged));
		return merged;
	}

	@Override
	public <B extends RosettaModelObjectBuilder> void mergeRosetta(B left, B right, Consumer<B> setter) {
		if (left != null && right != null) {
			left.merge(right, this);
		} else {
			ofNullable(right).ifPresent(setter);
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
	public <T> void mergeBasic(T left, T right, Consumer<T> setter, AttributeMeta... metas) {
		if (left != null && right != null) {
			if (!metaContains(metas, AttributeMeta.GLOBAL_KEY)) {
				throw new IllegalArgumentException(
						String.format("Attempting to merge 2 different basic values [left=%s, right=%s, type=%s]",
								left, right, left.getClass().getSimpleName()));
			}
		} else {
			ofNullable(right).ifPresent(setter);
		}
	}

	@Override
	public <T> void mergeBasic(List<T> left, List<T> right, Consumer<T> add) {
		emptyIfNull(right).forEach(add);
	}

	private boolean metaContains(AttributeMeta[] metas, AttributeMeta attributeMeta) {
		return Arrays.stream(metas).anyMatch(m -> m == attributeMeta);
	}
}
