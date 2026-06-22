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
import com.rosetta.model.lib.transform.Enrich;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import org.finos.rune.mapper.RuneJsonObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;

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

    private TransformObjectMapperFactory() {
    }

    /**
     * Builds the deserializing mapper for an {@link Ingest}-annotated function.
     * <p>
     * Note that the {@code CSV_LABELLED} format additionally needs a {@link LabelProvider}, which is not
     * carried by the {@link Ingest} annotation itself but by a separate {@code @RuneLabelProvider}
     * annotation on the function class. Use {@link #forTransformFunction(Class, ClassLoader)} to build a
     * {@code CSV_LABELLED} mapper.
     */
    public static ObjectMapper forIngest(Ingest ingest, ClassLoader classLoader) {
        Objects.requireNonNull(ingest, "ingest annotation must not be null");
        return create(ingest.format(), ingest.configPath(), classLoader);
    }

    /**
     * Builds the serializing mapper for a {@link Projection}-annotated function.
     * <p>
     * Note that the {@code CSV_LABELLED} format additionally needs a {@link LabelProvider}, which is not
     * carried by the {@link Projection} annotation itself but by a separate {@code @RuneLabelProvider}
     * annotation on the function class. Use {@link #forTransformFunction(Class, ClassLoader)} to build a
     * {@code CSV_LABELLED} mapper.
     */
    public static ObjectMapper forProjection(Projection projection, ClassLoader classLoader) {
        Objects.requireNonNull(projection, "projection annotation must not be null");
        return create(projection.format(), projection.configPath(), classLoader);
    }

    /**
     * Reflectively reads the {@link Ingest}/{@link Projection} annotation off the given function class and
     * builds the corresponding mapper, resolving resources against the function class's own classloader.
     */
    public static ObjectMapper forTransformFunction(Class<?> functionClass) {
        return forTransformFunction(functionClass, functionClass.getClassLoader());
    }

    /**
     * Reflectively reads the {@link Ingest}/{@link Projection} annotation off the given function class and
     * builds the corresponding mapper, resolving resources against the supplied classloader.
     * <p>
     * For the {@code CSV_LABELLED} format, the required {@link LabelProvider} is derived from the
     * {@code @RuneLabelProvider} annotation the Rune code generator places on the function class (via
     * {@link LabelProviderResolver}).
     *
     * @throws IllegalArgumentException if the class carries no {@code @Ingest}/{@code @Projection}
     *         annotation, or is an {@code @Enrich} transform (which does not (de)serialize); or if the
     *         format is {@code CSV_LABELLED} but the function class carries no {@code @RuneLabelProvider}.
     */
    public static ObjectMapper forTransformFunction(Class<?> functionClass, ClassLoader classLoader) {
        Objects.requireNonNull(functionClass, "functionClass must not be null");
        Ingest ingest = functionClass.getAnnotation(Ingest.class);
        if (ingest != null) {
            return create(ingest.format(), ingest.configPath(), classLoader, functionClass);
        }
        Projection projection = functionClass.getAnnotation(Projection.class);
        if (projection != null) {
            return create(projection.format(), projection.configPath(), classLoader, functionClass);
        }
        if (functionClass.isAnnotationPresent(Enrich.class)) {
            throw new IllegalArgumentException("Transform function " + functionClass.getName()
                    + " is annotated with @Enrich, which does not (de)serialize and therefore has no ObjectMapper");
        }
        throw new IllegalArgumentException("Transform function " + functionClass.getName()
                + " is not annotated with @Ingest or @Projection");
    }

    /**
     * Core per-format mapper construction shared with {@code TestPackUtils#getObjectMapper}. A {@code null}
     * classloader preserves the legacy classpath-resource lookup (Guava {@link Resources}); a non-null
     * classloader resolves the XML config and model types against that loader.
     * <p>
     * The {@code CSV_LABELLED} format cannot be built from the format alone — it needs a
     * {@link LabelProvider} resolved from the function class. Use
     * {@link #forTransformFunction(Class, ClassLoader)} for that format.
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
            case CSV_LABELLED:
                return RosettaObjectMapperCreator.forCSV(resolveLabelProvider(functionClass)).create();
            case XML:
                return createXmlMapper(configPath, classLoader);
            default:
                throw new IllegalArgumentException("Unsupported serialization format: " + format);
        }
    }

    /**
     * Resolves the {@link LabelProvider} for a {@code CSV_LABELLED} mapper from the {@code @RuneLabelProvider}
     * annotation on the function class.
     */
    @SuppressWarnings("unchecked")
    private static LabelProvider resolveLabelProvider(Class<?> functionClass) {
        if (functionClass == null) {
            throw new IllegalArgumentException("CSV_LABELLED requires a LabelProvider resolved from the "
                    + "transform function and cannot be built from the format alone. Use "
                    + "forTransformFunction(Class, ClassLoader) instead.");
        }
        if (!RosettaFunction.class.isAssignableFrom(functionClass)) {
            throw new IllegalArgumentException("Transform function " + functionClass.getName()
                    + " does not implement RosettaFunction and therefore cannot carry a @RuneLabelProvider");
        }
        LabelProvider labelProvider = LabelProviderResolver.fromTransformFunction(
                (Class<? extends RosettaFunction>) functionClass);
        if (labelProvider == null) {
            throw new IllegalArgumentException("CSV_LABELLED requires a LabelProvider, but transform function "
                    + functionClass.getName() + " carries no @RuneLabelProvider annotation");
        }
        return labelProvider;
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
