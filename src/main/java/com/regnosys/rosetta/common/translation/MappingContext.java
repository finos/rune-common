package com.regnosys.rosetta.common.translation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A new MappingContext is created for each ingested file to hold any mapping state.
 */
public class MappingContext {

	private final List<Mapping> mappings;
	private final Map<Object, Object> mappingParams;
	// Execute mapping on separate thread pool
	private final ExecutorService executor =
			Executors.newFixedThreadPool(5,
					new ThreadFactoryBuilder().setNameFormat("mapper-%d").build());
	// Collect any tasks invoked during mapping so we can wait until they're complete before continuing
	private final List<CompletableFuture<?>> invokedTasks = new ArrayList<>();
	
	private final List<String> mappingErrors = new ArrayList<>();

	public MappingContext() {
		this(new ArrayList<>(), new ConcurrentHashMap<>());
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

	public List<CompletableFuture<?>> getInvokedTasks() {
		return invokedTasks;
	}

	public List<String> getMappingErrors() {
		return mappingErrors;
	}
}
