package com.regnosys.rosetta.common.serialisation;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;

import java.util.Objects;
import java.util.Optional;

/**
 * Convenience entry points that resolve <em>and</em> construct the {@link ObjectMapper} for a transform
 * function's input or output in one step, driven by the {@link Ingest}/{@link Projection} annotation the
 * Rune code generator places on the generated function class.
 * <p>
 * These combine the two halves of transform serialization — the pure decision
 * ({@link TransformSerializationResolver}) and the construction ({@link ClasspathTransformMapperFactory})
 * — for the common classpath case.
 * <p>
 * <b>Classloader caution:</b> construction resolves resources against the function class's own
 * {@link ClassLoader}, and the built mapper holds references into it. Runtimes that load models in
 * isolated, disposable classloaders must not build mappers through these classpath conveniences —
 * they implement {@link TransformMapperFactory} on the component that owns the model classloader, so
 * mappers share its cache and lifecycle (see the {@link TransformMapperFactory} javadoc).
 */
public final class TransformObjectMapperFactory {

    private static final ClasspathTransformMapperFactory FACTORY = new ClasspathTransformMapperFactory();

    private TransformObjectMapperFactory() {
    }

    /**
     * Builds the mapper that <b>deserializes the input</b> of a transform function, from the
     * {@link Ingest} annotation the Rune code generator places on it. Resources resolve against the
     * function class's own classloader. See {@link #inputForTransformFunction(Class, ClassLoader)}.
     */
    public static Optional<ObjectMapper> inputForTransformFunction(Class<?> functionClass) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        return TransformSerializationResolver.input(functionClass)
                .map(serialization -> FACTORY.create(serialization, functionClass));
    }

    /**
     * Builds the mapper that <b>deserializes the input</b> of a transform function, from its
     * {@link Ingest} annotation, resolving resources against the supplied classloader.
     * <p>
     * The serialized (annotation-described) side of a transform input is the {@code @Ingest} of a
     * translate. Returns {@link Optional#empty()} when the class carries no {@code @Ingest} — e.g. the
     * (non-serialized, plain JSON) input of a projection, an {@code @Enrich} transform, or a non-transform
     * function — so callers can fall back (to a default mapper, …) without catching an exception.
     * <p>
     * For the {@code CSV_LABELLED} format the {@code LabelProvider} is derived from the
     * {@code @RuneLabelProvider} annotation the Rune code generator places on every transform function; if
     * that annotation is absent (e.g. a hand-written / non-generated function), the mapper falls back to
     * plain (unlabelled) CSV rather than failing.
     */
    public static Optional<ObjectMapper> inputForTransformFunction(Class<?> functionClass, ClassLoader classLoader) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        return TransformSerializationResolver.input(functionClass)
                .map(s -> withClassLoader(classLoader).create(s, functionClass));
    }

    /**
     * Builds the mapper that <b>serializes the output</b> of a transform function, from the
     * {@link Projection} annotation the Rune code generator places on it. Resources resolve against the
     * function class's own classloader. See {@link #outputForTransformFunction(Class, ClassLoader)}.
     */
    public static Optional<ObjectMapper> outputForTransformFunction(Class<?> functionClass) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        return TransformSerializationResolver.output(functionClass)
                .map(serialization -> FACTORY.create(serialization, functionClass));
    }

    /**
     * Builds the mapper that <b>serializes the output</b> of a transform function, from its
     * {@link Projection} annotation, resolving resources against the supplied classloader.
     * <p>
     * The serialized (annotation-described) side of a transform output is the {@code @Projection} of a
     * projection. Returns {@link Optional#empty()} when the class carries no {@code @Projection} — e.g. the
     * (non-serialized, plain JSON) output of a translate, an {@code @Enrich} transform, or a non-transform
     * function — so callers can fall back (to a default mapper, …) without catching an exception.
     * <p>
     * {@code CSV_LABELLED} labels are resolved as described on {@link #inputForTransformFunction(Class, ClassLoader)}.
     */
    public static Optional<ObjectMapper> outputForTransformFunction(Class<?> functionClass, ClassLoader classLoader) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        return TransformSerializationResolver.output(functionClass)
                .map(s -> withClassLoader(classLoader).create(s, functionClass));
    }

    /**
     * Per-format mapper construction from an explicit format and config path.
     *
     * @deprecated use a {@link TransformMapperFactory} (on the classpath:
     *         {@link ClasspathTransformMapperFactory}) with a {@link TransformSerialization} — that seam
     *         also lets isolated-classloader runtimes own the construction. A {@code null} classloader
     *         here preserves the legacy Guava classpath-resource lookup. Because no function class is
     *         supplied, {@code CSV_LABELLED} falls back to plain (unlabelled) CSV.
     */
    @Deprecated
    public static ObjectMapper create(SerializationFormat format, String configPath, ClassLoader classLoader) {
        return withClassLoader(classLoader).create(new TransformSerialization(format, configPath), null);
    }

    /**
     * A classpath factory resolving against an explicit classloader instead of the function class's own.
     * A {@code null} loader preserves the legacy Guava classpath-resource lookup.
     */
    private static ClasspathTransformMapperFactory withClassLoader(ClassLoader classLoader) {
        return new ClasspathTransformMapperFactory() {
            @Override
            protected ClassLoader classLoader(Class<?> functionClass) {
                return classLoader;
            }
        };
    }
}
