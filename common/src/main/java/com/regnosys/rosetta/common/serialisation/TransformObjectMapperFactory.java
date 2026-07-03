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
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.transform.LabelProviderResolver;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the {@link ObjectMapper} that (de)serializes the input or output of a transform function,
 * driven by the {@link Ingest}/{@link Projection} annotation the Rune code generator places on the
 * generated function class.
 * <p>
 * This is the annotation-driven replacement for {@code TestPackUtils#getObjectMapper} (which is driven
 * by the {@code inputSerialisation}/{@code outputSerialisation} entries of a pipeline JSON). Both paths
 * share the per-format mapper construction in {@link RosettaObjectMapperCreator}; the annotation simply
 * makes the model the single source of truth for the format and config file.
 * <p>
 * The function's own {@link ClassLoader} is threaded through so that the XML configuration resource and
 * the generated model types resolve against the model on the classpath rather than this library's loader.
 * <p>
 * For the {@code CSV_LABELLED} format the required {@link LabelProvider} is also derived from the function
 * class, via the {@code @RuneLabelProvider} annotation the Rune code generator places on it (resolved by
 * {@link LabelProviderResolver}).
 */
public final class TransformObjectMapperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformObjectMapperFactory.class);

    private TransformObjectMapperFactory() {
    }

    /**
     * Builds the mapper that <b>deserializes the input</b> of a transform function, from the
     * {@link Ingest} annotation the Rune code generator places on it. Resources resolve against the
     * function class's own classloader. See {@link #inputForTransformFunction(Class, ClassLoader)}.
     */
    public static Optional<ObjectMapper> inputForTransformFunction(Class<?> functionClass) {
        return inputForTransformFunction(functionClass, functionClass.getClassLoader());
    }

    /**
     * Builds the mapper that <b>deserializes the input</b> of a transform function, from its
     * {@link Ingest} annotation, resolving resources against the supplied classloader.
     * <p>
     * The serialized (annotation-described) side of a transform input is the {@code @Ingest} of a
     * translate. Returns {@link Optional#empty()} when the class carries no {@code @Ingest} — e.g. the
     * (non-serialized, plain JSON) input of a projection, an {@code @Enrich} transform, or a non-transform
     * function — so callers can fall back (to a pipeline config, a default mapper, …) without catching an
     * exception.
     * <p>
     * For the {@code CSV_LABELLED} format the {@link LabelProvider} is derived from the
     * {@code @RuneLabelProvider} annotation the Rune code generator places on every transform function; if
     * that annotation is absent (e.g. a hand-written / non-generated function), the mapper falls back to
     * plain (unlabelled) CSV rather than failing.
     */
    public static Optional<ObjectMapper> inputForTransformFunction(Class<?> functionClass, ClassLoader classLoader) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        Ingest ingest = functionClass.getAnnotation(Ingest.class);
        if (ingest != null) {
            return Optional.of(create(ingest.format(), ingest.configPath(), classLoader, functionClass));
        }
        return Optional.empty();
    }

    /**
     * Builds the mapper that <b>serializes the output</b> of a transform function, from the
     * {@link Projection} annotation the Rune code generator places on it. Resources resolve against the
     * function class's own classloader. See {@link #outputForTransformFunction(Class, ClassLoader)}.
     */
    public static Optional<ObjectMapper> outputForTransformFunction(Class<?> functionClass) {
        return outputForTransformFunction(functionClass, functionClass.getClassLoader());
    }

    /**
     * Builds the mapper that <b>serializes the output</b> of a transform function, from its
     * {@link Projection} annotation, resolving resources against the supplied classloader.
     * <p>
     * The serialized (annotation-described) side of a transform output is the {@code @Projection} of a
     * projection. Returns {@link Optional#empty()} when the class carries no {@code @Projection} — e.g. the
     * (non-serialized, plain JSON) output of a translate, an {@code @Enrich} transform, or a non-transform
     * function — so callers can fall back (to a pipeline config, a default mapper, …) without catching an
     * exception.
     * <p>
     * {@code CSV_LABELLED} labels are resolved as described on {@link #inputForTransformFunction(Class, ClassLoader)}.
     */
    public static Optional<ObjectMapper> outputForTransformFunction(Class<?> functionClass, ClassLoader classLoader) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        Projection projection = functionClass.getAnnotation(Projection.class);
        if (projection != null) {
            return Optional.of(create(projection.format(), projection.configPath(), classLoader, functionClass));
        }
        return Optional.empty();
    }

    /**
     * Core per-format mapper construction shared with {@code TestPackUtils#getObjectMapper}. A {@code null}
     * classloader preserves the legacy classpath-resource lookup (Guava {@link Resources}); a non-null
     * classloader resolves the XML config and model types against that loader.
     * <p>
     * The {@code CSV_LABELLED} format needs a {@link LabelProvider} resolved from the function class; when
     * called without one (as here), it falls back to plain (unlabelled) CSV. Use
     * {@link #inputForTransformFunction(Class, ClassLoader)} / {@link #outputForTransformFunction(Class, ClassLoader)}
     * to get labelled CSV.
     */
    public static ObjectMapper create(SerializationFormat format, String configPath, ClassLoader classLoader) {
        return create(format, configPath, classLoader, null);
    }

    private static ObjectMapper create(SerializationFormat format, String configPath, ClassLoader classLoader,
                                       Class<?> functionClass) {
        Objects.requireNonNull(format, "serialization format must not be null");
        switch (format) {
            case JSON:
                return RosettaObjectMapper.getNewRosettaObjectMapper();
            case RUNE_JSON:
                return new RuneJsonObjectMapper();
            case CSV:
                return RosettaObjectMapperCreator.forCSV().create();
            case CSV_LABELLED: {
                LabelProvider labelProvider = resolveLabelProvider(functionClass);
                if (labelProvider == null) {
                    LOGGER.warn("CSV_LABELLED requested but no @RuneLabelProvider could be resolved{}; "
                            + "falling back to unlabelled CSV.",
                            functionClass != null ? " from " + functionClass.getName() : "");
                    return RosettaObjectMapperCreator.forCSV().create();
                }
                return RosettaObjectMapperCreator.forCSV(labelProvider).create();
            }
            case XML:
                return createXmlMapper(configPath, classLoader);
            default:
                throw new IllegalArgumentException("Unsupported serialization format: " + format);
        }
    }

    /**
     * Tries to resolve the {@link LabelProvider} for a {@code CSV_LABELLED} mapper from the
     * {@code @RuneLabelProvider} annotation on the function class. Returns {@code null} when none is
     * available — no function class, the class is not a {@link RosettaFunction}, or it carries no
     * {@code @RuneLabelProvider} (e.g. a hand-written / non-generated function) — so the caller can fall
     * back to unlabelled CSV.
     */
    @SuppressWarnings("unchecked")
    private static LabelProvider resolveLabelProvider(Class<?> functionClass) {
        if (functionClass == null || !RosettaFunction.class.isAssignableFrom(functionClass)) {
            return null;
        }
        return LabelProviderResolver.fromTransformFunction((Class<? extends RosettaFunction>) functionClass);
    }

    private static ObjectMapper createXmlMapper(String configPath, ClassLoader classLoader) {
        if (configPath == null || configPath.isEmpty()) {
            // A bare `[ingest XML]` with no schema/config: use an empty XML configuration.
            return RosettaObjectMapperCreator
                    .forXML(new RosettaXMLConfiguration(Collections.emptyMap()), resolveClassLoader(classLoader))
                    .create();
        }
        URL configUrl = (classLoader != null) ? classLoader.getResource(configPath) : Resources.getResource(configPath);
        if (configUrl == null) {
            throw new IllegalStateException("Could not find XML configuration '" + configPath + "' on the classpath");
        }
        try (InputStream inputStream = configUrl.openStream()) {
            return RosettaObjectMapperCreator.forXML(inputStream, resolveClassLoader(classLoader)).create();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read XML configuration '" + configPath + "'", e);
        }
    }

    private static ClassLoader resolveClassLoader(ClassLoader classLoader) {
        return classLoader != null ? classLoader : TransformObjectMapperFactory.class.getClassLoader();
    }
}
