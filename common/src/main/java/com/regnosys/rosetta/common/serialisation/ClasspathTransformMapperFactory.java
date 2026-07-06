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
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.transform.LabelProviderResolver;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
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

/**
 * The default {@link TransformMapperFactory}: per-format construction resolving the serialization config
 * and model types against the function class's own {@link ClassLoader} (falling back to this library's).
 * <p>
 * Suitable wherever the model lives on the application classpath — tests, model builds, the pipeline
 * test-pack runner. It is <b>not</b> suitable for runtimes that load models in isolated, disposable
 * classloaders: there the {@link TransformMapperFactory} must be implemented by the component owning the
 * model classloader, so constructed mappers share its cache and lifecycle (see the interface javadoc).
 * <p>
 * For the {@code CSV_LABELLED} format the required {@link LabelProvider} is derived from the
 * {@code @RuneLabelProvider} annotation the Rune code generator places on the function class; when it
 * cannot be resolved (e.g. a hand-written function), the mapper degrades to plain (unlabelled) CSV
 * rather than failing.
 */
public class ClasspathTransformMapperFactory implements TransformMapperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathTransformMapperFactory.class);

    @Override
    public ObjectMapper create(TransformSerialization serialization, Class<?> functionClass) {
        Objects.requireNonNull(serialization, "serialization must not be null");
        ClassLoader classLoader = functionClass != null ? functionClass.getClassLoader() : null;
        return build(serialization.getFormat(), serialization.getConfigPath(), classLoader, functionClass);
    }

    /**
     * Shared per-format construction. A {@code null} classloader preserves the legacy Guava
     * {@link Resources} classpath lookup (used by the deprecated pipeline-serialisation path); a non-null
     * classloader resolves the XML config and model types against that loader.
     */
    static ObjectMapper build(SerializationFormat format, String configPath, ClassLoader classLoader,
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
     * {@code @RuneLabelProvider} — so the caller can fall back to unlabelled CSV.
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
        return classLoader != null ? classLoader : ClasspathTransformMapperFactory.class.getClassLoader();
    }
}
