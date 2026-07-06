package com.regnosys.rosetta.common.serialisation;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.transform.SerializationFormat;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link TransformMapperFactory} decorator that builds each distinct {@link TransformSerialization}
 * once and caches it, so implementors get per-instance mapper reuse without writing any caching
 * themselves.
 * <p>
 * The cache lives and dies with this factory instance, and cached mappers may hold references into the
 * classloader they were built against. So the factory must be owned by the component whose model/
 * classloader scope it serves — one per model instance, one per test runner — and must <b>never</b> be
 * held statically, which would pin that classloader for the lifetime of the JVM.
 * <p>
 * {@code CSV_LABELLED} is deliberately not cached: its labels derive from the specific function class,
 * so a single serialization key cannot identify the mapper.
 */
public class CachingTransformMapperFactory implements TransformMapperFactory {

    private final TransformMapperFactory delegate;
    private final ConcurrentMap<TransformSerialization, ObjectMapper> cache = new ConcurrentHashMap<>();

    public CachingTransformMapperFactory(TransformMapperFactory delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public ObjectMapper create(TransformSerialization serialization, Class<?> functionClass) {
        if (serialization.getFormat() == SerializationFormat.CSV_LABELLED) {
            return delegate.create(serialization, functionClass);
        }
        return cache.computeIfAbsent(serialization, key -> delegate.create(key, functionClass));
    }
}
