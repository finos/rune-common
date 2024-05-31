package com.regnosys.rosetta.common.translation;

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

import com.google.common.annotations.VisibleForTesting;
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
    private final SynonymToEnumMap synonymToEnumMap;
    // Execute mapping on separate thread pool
    private final ExecutorService executor;
    // Collect any tasks invoked during mapping so we can wait until they're complete before continuing
    private final List<CompletableFuture<?>> invokedTasks = new ArrayList<>();

    private final List<String> mappingErrors = new ArrayList<>();

    public MappingContext(Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap) {
        this(new ArrayList<>(), new ConcurrentHashMap<>(), synonymToEnumMap);
    }

    // Unit testing
    @VisibleForTesting
    public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams, Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap) {
        this(mappings,
                mappingParams,
                synonymToEnumMap,
                Executors.newFixedThreadPool(5, new ThreadFactoryBuilder().setNameFormat("mapper-%d").build()));
    }

    public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams, Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap, ExecutorService executor) {
        this.mappings = mappings;
        this.mappingParams = mappingParams;
        this.synonymToEnumMap = new SynonymToEnumMap(synonymToEnumMap);
        this.executor = executor;
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
}
