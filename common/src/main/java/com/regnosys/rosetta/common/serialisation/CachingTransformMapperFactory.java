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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link TransformMapperFactory} decorator that builds each distinct mapper once and caches it.
 * Mapper construction is not cheap — Jackson module registration, and for XML a full parse of the
 * serialization config — while a runtime requests a mapper on every transform execution, so
 * implementors get per-instance reuse without writing any caching themselves.
 * <p>
 * The cache key pairs the {@link TransformSerialization} with the part of the function-class context
 * the format is actually sensitive to, so equal serializations are shared exactly as widely as is
 * correct:
 * <ul>
 *   <li>{@code CSV_LABELLED} — the function class itself: the labels derive from the function's
 *       {@code @RuneLabelProvider}, so each labelled function gets (and reuses) its own mapper.</li>
 *   <li>{@code RUNE_JSON} and {@code XML} — the function class's {@link ClassLoader}: these mappers
 *       resolve model types against it, so functions from the same model share one mapper while
 *       models in different classloaders never cross.</li>
 *   <li>{@code JSON} and {@code CSV} — nothing: one mapper per factory.</li>
 * </ul>
 * <p>
 * The cache lives and dies with this factory instance, and cached mappers may hold references into the
 * classloader they were built against. So the factory must be owned by the component whose model/
 * classloader scope it serves — one per model instance, one per test runner — and must <b>never</b> be
 * held statically, which would pin that classloader for the lifetime of the JVM. An owner that replaces
 * its model classloader in place (e.g. a workspace recompile) must call {@link #clear()} at that point,
 * otherwise a stale mapper built against the discarded classloader can be served.
 */
public class CachingTransformMapperFactory implements TransformMapperFactory {

    private final TransformMapperFactory delegate;
    private final ConcurrentMap<CacheKey, ObjectMapper> cache = new ConcurrentHashMap<>();

    public CachingTransformMapperFactory(TransformMapperFactory delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public ObjectMapper create(TransformSerialization serialization, Class<?> functionClass) {
        CacheKey key = new CacheKey(serialization, cacheScope(serialization, functionClass));
        return cache.computeIfAbsent(key, k -> delegate.create(serialization, functionClass));
    }

    /**
     * Drops every cached mapper, so the next request rebuilds against the delegate's current state.
     * Call when the model classloader this factory serves is replaced in place.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * The part of the function-class context that distinguishes cached mappers for the given
     * serialization — see the class doc for the per-format rationale.
     */
    private static Object cacheScope(TransformSerialization serialization, Class<?> functionClass) {
        switch (serialization.getFormat()) {
            case CSV_LABELLED:
                return functionClass;
            case RUNE_JSON:
            case XML:
                return functionClass != null ? functionClass.getClassLoader() : null;
            default:
                return null;
        }
    }

    private static final class CacheKey {
        private final TransformSerialization serialization;
        private final Object scope;

        private CacheKey(TransformSerialization serialization, Object scope) {
            this.serialization = serialization;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) o;
            return serialization.equals(other.serialization) && Objects.equals(scope, other.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serialization, scope);
        }
    }
}
