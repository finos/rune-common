package com.regnosys.rosetta.common.translation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.inject.Module;

/**
 * A new MappingContext is created for each ingested file to hold any mapping state.
 */
public class MappingContext {

    private final List<Mapping> mappings;
    private final Map<Object, Object> mappingParams;
    private final SynonymToEnumMap synonymToEnumMap;
    // Execute mapping on separate thread pool
    private final ExecutorService executor;
    private final Module runtimeModule;
    // Collect any tasks invoked during mapping so we can wait until they're complete before continuing
    private final List<CompletableFuture<?>> invokedTasks = new ArrayList<>();

    private final List<String> mappingErrors = new ArrayList<>();

    public MappingContext(Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, Module runtimeModule) {
        this(new ArrayList<>(), new ConcurrentHashMap<>(), synonymToEnumMap, runtimeModule);
    }

    // Unit testing
    public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams, Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, Module runtimeModule) {
        this(mappings,
                mappingParams,
                synonymToEnumMap,
                Executors.newFixedThreadPool(5, new ThreadFactoryBuilder().setNameFormat("mapper-%d").build()),
                runtimeModule);
    }

    public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams, Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, ExecutorService executor, Module runtimeModule) {
        this.mappings = mappings;
        this.mappingParams = mappingParams;
        this.synonymToEnumMap = new SynonymToEnumMap(synonymToEnumMap);
        this.executor = executor;
        this.runtimeModule = runtimeModule;
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

    public SynonymToEnumMap getSynonymToEnumMap() {
        return synonymToEnumMap;
    }

    public Module getRuntimeModule() {
        return runtimeModule;
    }
}
