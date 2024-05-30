package com.regnosys.rosetta.common.merging;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.rosetta.util.CollectionUtils.emptyIfNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Simple implementation of BuilderMerger interface that merges two objects together.
 */
public class SimpleMerger implements BuilderMerger {

	private final Consumer<RosettaModelObjectBuilder> postProcessor;

	public SimpleMerger() {
		this(null);
	}

	public SimpleMerger(Consumer<RosettaModelObjectBuilder> postProcessor) {
		this.postProcessor = postProcessor;
	}

	@Override
	public <T extends RosettaModelObjectBuilder> void run(T o1, T o2) {
		// merge o2 into o1
		T merged = requireNonNull(o1).merge(requireNonNull(o2), this).prune();
		// optionally post process
		Optional.ofNullable(postProcessor).ifPresent(p -> p.accept(merged));
	}

	@Override
	public <T extends RosettaModelObjectBuilder> void mergeRosetta(T o1, T o2, Consumer<T> o1Setter) {
		if (o1 != null && o2 != null) {
			// if both o1 and o2 are present, then we need to merge
			o1.merge(o2, this);
		} else {
			// if o2 is present and o1 is absent, then we can just overwrite
			ofNullable(o2).ifPresent(o1Setter);
		}
	}

	@Override
	public <T extends RosettaModelObjectBuilder> void mergeRosetta(List<? extends T> o1, List<? extends T> o2, Function<Integer, T> o1GetOrCreateByIndex) {
		// merge list items with matching indexes, e.g. object at list o1 index 0, merged with object at list o2 index 0, and so on..
		// iterate through a copy of the lists to prevent a ConcurrentModificationException.
		int i = 0;
		for (Iterator<? extends T> i2 = emptyIfNull(o2).iterator(); i2.hasNext(); i++) {
			if (i2.hasNext()) {
				o1GetOrCreateByIndex.apply(i).merge(i2.next(), this);
			}
		}
	}

	@Override
	public <T> void mergeBasic(T o1, T o2, Consumer<T> o1Setter, AttributeMeta... metas) {
		if (o1 != null && o2 != null && !o1.equals(o2)) {
			if (!metaContains(metas, AttributeMeta.GLOBAL_KEY)) {
				throw new IllegalArgumentException(
						String.format("Attempting to merge 2 different basic values [o1=%s, o2=%s, type=%s]",
								o1, o2, o1.getClass().getSimpleName()));
			}
		} else {
			// if o1 is absent and o2 is present, then we can just overwrite
			ofNullable(o2).ifPresent(o1Setter);
		}
	}

	@Override
	public <T> void mergeBasic(List<? extends T> o1, List<? extends T> o2, Consumer<T> o1Add) {
		// merge lists
		emptyIfNull(o2).forEach(o1Add);
	}

	private boolean metaContains(AttributeMeta[] metas, AttributeMeta attributeMeta) {
		return Arrays.stream(metas).anyMatch(m -> m == attributeMeta);
	}
}
