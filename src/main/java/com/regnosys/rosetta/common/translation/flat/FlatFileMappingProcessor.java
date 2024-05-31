package com.regnosys.rosetta.common.translation.flat;

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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.regnosys.rosetta.common.translation.Mapping;
import com.regnosys.rosetta.common.translation.MappingContext;
import com.regnosys.rosetta.common.translation.MappingProcessor;
import com.regnosys.rosetta.common.translation.Path;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Mapping processor base type specialised for flat, or shallow, xml files where all fields are translated
 * by a single mapping processor rather than synonyms.
 */
public abstract class FlatFileMappingProcessor<TYPE extends RosettaModelObjectBuilder> extends MappingProcessor {

	protected static final Path BASE_PATH = Path.parse("WorkflowStep");

	protected final DateTimeFormatter dateParser = DateTimeFormatter.BASIC_ISO_DATE;
	protected final DateTimeFormatter localTimeParser = DateTimeFormatter.ISO_LOCAL_TIME;

	@FunctionalInterface
	protected interface MappingConsumer<T> {
		List<PathValue<?>> accept(Map<String, Integer> indexes, String xmlValue, PathValue<T> pathValue);
	}

	protected static class PathValue<T> {
		private final Path modelPath;
		private final T value;
		private final boolean conditional;

		public PathValue(Path modelPath, T value) {
			this(modelPath, value, false);
		}

		public PathValue(Path modelPath, T value, boolean conditional) {
			this.modelPath = modelPath;
			this.value = value;
			this.conditional = conditional;
		}

		public Path getModelPath() {
			return modelPath;
		}

		public T getValue() {
			return value;
		}
	}

	private Multimap<IndexCapturePath, IndexCapturePath> pathLookup = ArrayListMultimap.create();
	private Multimap<IndexCapturePath, MappingConsumer<TYPE>> mappings = HashMultimap.create();
	private Multimap<String, Capture> captures = HashMultimap.create();
	private Collection<BiConsumer<Multimap<String, Capture>, TYPE>> postCaptureProcessors = new ArrayList<>();

	public FlatFileMappingProcessor(RosettaPath modelPath, List<Path> synonymPaths, MappingContext context) {
		super(modelPath, synonymPaths, context);
	}

	protected BigDecimal parseDecimal(String value) {
		return new BigDecimal(value);
	}

	/**
	 * Format "yyyyMMdd"
	 */
	protected Date parseISODate(String value) {
		return Date.of(LocalDate.parse(value, dateParser));
	}

	@Override
	public void map(Path synonymPath, Optional<RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent) {
		@SuppressWarnings("unchecked")
		TYPE type = (TYPE) parent;
		Set<Mapping> inputs = new HashSet(this.getContext().getMappings());
		doHardCodings(type);

		List<Mapping> allMappings = new ArrayList<>();
		for (Mapping m : inputs) {
			String xmlPath = m.getXmlPath().toString();
			IndexCapturePath xmlCapturing = IndexCapturePath.parse(xmlPath);
			Collection<IndexCapturePath> capturers = pathLookup.get(xmlCapturing.toUnindexed());
			boolean mapped = false;
			for (IndexCapturePath capturer : capturers) {
				if (xmlCapturing.matches(capturer)) {
					Map<String, Integer> captureIndexes = capturer.captureIndexes(xmlCapturing);
					//The way we build paths for Json files is broken so it just shoves the indexes at the end of the path - so I am going to capture that index as "other"
					xmlCapturing.getLastIndex().ifPresent(i -> captureIndexes.put("other", i));
					Collection<MappingConsumer<TYPE>> mappingConsumers = mappings.get(capturer);
					String xmlValue = m.getXmlValue() == null ? null : m.getXmlValue().toString();
					for (MappingConsumer<TYPE> mc : mappingConsumers) {
						List<PathValue<?>> results = mc.accept(captureIndexes, xmlValue, new PathValue<>(BASE_PATH, type));
						for (PathValue<?> r : results) {
							if (xmlValue != null) {
								allMappings.add(new Mapping(m.getXmlPath(), xmlValue, r.modelPath, r.value, null, true, r.conditional, false));
							}
						}
					}
					mapped = true;
				}
			}
			if (!mapped) {
				allMappings.add(m);
			}
		}
		doConditionalStuff(type);
		updateMappings(allMappings);
	}

	private void updateMappings(List<Mapping> allMappings) {
		this.getContext().getMappings().clear();
		this.getContext().getMappings().addAll(allMappings);
	}

	@Override
	public <T> void mapBasic(Path synonymPath, Optional<T> instance, RosettaModelObjectBuilder parent) {
		super.map(synonymPath, Optional.empty(), parent);
	}

	@Override
	public void map(Path synonymPath, List<? extends RosettaModelObjectBuilder> builder,
			RosettaModelObjectBuilder parent) {
		super.map(synonymPath, Optional.empty(), parent);
	}

	protected abstract void doHardCodings(TYPE object);

	protected MappingConsumer<TYPE> nonNullConsumer(MappingConsumer<TYPE> consumer) {
		return (i, v, r) -> v != null ? consumer.accept(i, v, r) : Lists.newArrayList();
	}

	private void doConditionalStuff(TYPE workflow) {
		for (BiConsumer<Multimap<String, Capture>, TYPE> processor:postCaptureProcessors) {
			processor.accept(captures, workflow);
		}
	}

	protected <T> MappingConsumer<T> capture(String name) {
		return (indexes, value, workflow) -> {
			captures.put(name, new Capture(indexes, value));
			return Lists.newArrayList(new PathValue<>(workflow.getModelPath(), value, true));
		};
	}

	protected void addMapping(IndexCapturePath path, MappingConsumer<TYPE> consumer) {
		pathLookup.put(path.toUnindexed(), path);
		mappings.put(path, consumer);
	}

	protected void addPostCaptureProcessors(BiConsumer<Multimap<String, Capture>, TYPE> postCaptureProcessor) {
		postCaptureProcessors.add(postCaptureProcessor);
	}
	
	protected <A> Optional<A> any(Collection<A> collection) {
		return collection.stream().findAny();
	}
	
	protected Optional<Capture> matchingIndex(Capture toMatch, Collection<Capture> lookIn, String... matchOn) {
		Map<String, Integer> matchValues = Arrays.stream(matchOn).collect(Collectors.toMap(k->k, k->toMatch.getIndexes().get(k)));
		return lookIn.stream().filter(c->matches(c,matchValues)).findFirst();
	}

	private boolean matches(Capture c, Map<String, Integer> matchValues) {
		Map<String, Integer> indexes = c.getIndexes();
		for (Entry<String, Integer> matchValue : matchValues.entrySet()) {
			Integer val = indexes.get(matchValue.getKey());
			if (!val.equals(matchValue.getValue())) {
				return false;
			}
		}
		return true;
	}
}
