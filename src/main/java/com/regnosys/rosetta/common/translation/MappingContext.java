package com.regnosys.rosetta.common.translation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MappingContext {

	private final List<Mapping> mappings;
	private final Map<Object, Object> mappingParams;
	private final ExecutorService executor =
			Executors.newFixedThreadPool(5,
					new ThreadFactoryBuilder().setNameFormat("mapper-%d").build());

	public MappingContext() {
		this(new ArrayList<>(), new HashMap<>());
	}

	public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams) {
		this.mappings = mappings;
		this.mappingParams = mappingParams;
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public Map<Object, Object> getMappingParams() {
		return mappingParams;
	}

	public ExecutorService getExecutor() {
		return executor;
	}
}
