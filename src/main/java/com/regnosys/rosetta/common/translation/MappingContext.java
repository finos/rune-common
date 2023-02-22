package com.regnosys.rosetta.common.translation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;

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
    private final SynonymToEnumMap synonymToEnumMap;
    // Execute mapping on separate thread pool
    private final ExecutorService executor;
    private final Injector injector;
    // Collect any tasks invoked during mapping, so we can wait until they're complete before continuing
    private final List<CompletableFuture<?>> invokedTasks = new ArrayList<>();

    private final List<String> mappingErrors = new ArrayList<>();

    public MappingContext(Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, Injector injector) {
        this(new ArrayList<>(), new ConcurrentHashMap<>(), synonymToEnumMap, injector);
    }

    // Unit testing
    @VisibleForTesting
    public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams, Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, Injector injector) {
        this(mappings,
                mappingParams,
                synonymToEnumMap,
                Executors.newFixedThreadPool(5, new ThreadFactoryBuilder().setNameFormat("mapper-%d").build()),
                injector);
    }

    public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams, Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, ExecutorService executor, Injector injector) {
        this.mappings = mappings;
        this.mappingParams = mappingParams;
        this.synonymToEnumMap = new SynonymToEnumMap(synonymToEnumMap);
        this.executor = executor;
        this.injector = injector;
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

    public Injector getInjector() {
        return injector;
    }
}
