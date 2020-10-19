package com.regnosys.rosetta.common.merging;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.rosetta.util.CollectionUtils.*;

public class SimpleUnmerger implements BuilderMerger {

	private final Consumer<RosettaModelObjectBuilder> postProcessor;

	public SimpleUnmerger() {
		this(null);
	}

	public SimpleUnmerger(Consumer<RosettaModelObjectBuilder> postProcessor) {
		this.postProcessor = postProcessor;
	}

	@Override
	public <B extends RosettaModelObjectBuilder> B run(B instance, B template) {
		B unmerged = instance.merge(template, this).prune();
		Optional.ofNullable(postProcessor).ifPresent(p -> p.accept(unmerged));
		return unmerged;
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
	public <T> void mergeBasic(T left, T right, Consumer<T> setter, AttributeMeta... metas) {
		if (left != null && right != null) {
			setter.accept(null);
		}
	}

	@Override
	public <T> void mergeBasic(List<T> left, List<T> right, Consumer<T> add) {
		emptyIfNull(left).removeAll(emptyIfNull(right));
	}
}
