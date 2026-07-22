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
 * Suitable as-is wherever the model lives on the application classpath — tests, model builds, the
 * pipeline test-pack runner. Runtimes that load models in isolated, disposable classloaders should
 * <b>extend</b> this class rather than reimplement the per-format construction: the classloader-specific
 * concerns are isolated in protected hooks — {@link #defaultClassLoader()} (the model loader to use when
 * no function class is resolvable) and {@link #openXmlConfig(String, Class)} (where the XML config is
 * looked up) — and caching comes from wrapping in a {@link CachingTransformMapperFactory}. Everything
 * else (which mapper implements which format, the {@code CSV_LABELLED} label resolution) is inherited.
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
        switch (serialization.getFormat()) {
            case JSON:
                return jsonMapper();
            case RUNE_JSON:
                return runeJsonMapper(functionClass);
            case CSV:
                return csvMapper();
            case CSV_LABELLED:
                return csvLabelledMapper(functionClass);
            case XML:
                return xmlMapper(serialization.getConfigPath(), functionClass);
            default:
                throw new IllegalArgumentException("Unsupported serialization format: " + serialization.getFormat());
        }
    }

    protected ObjectMapper jsonMapper() {
        return RosettaObjectMapper.getNewRosettaObjectMapper();
    }

    protected ObjectMapper runeJsonMapper(Class<?> functionClass) {
        // Rune JSON resolves model types (e.g. for global-key hashing) so it needs the model classloader.
        ClassLoader classLoader = classLoader(functionClass);
        return classLoader != null ? new RuneJsonObjectMapper(classLoader) : new RuneJsonObjectMapper();
    }

    protected ObjectMapper csvMapper() {
        return RosettaObjectMapperCreator.forCSV().create();
    }

    protected ObjectMapper csvLabelledMapper(Class<?> functionClass) {
        LabelProvider labelProvider = resolveLabelProvider(functionClass);
        if (labelProvider == null) {
            LOGGER.warn("CSV_LABELLED requested but no @RuneLabelProvider could be resolved{}; "
                    + "falling back to unlabelled CSV.",
                    functionClass != null ? " from " + functionClass.getName() : "");
            return csvMapper();
        }
        return RosettaObjectMapperCreator.forCSV(labelProvider).create();
    }

    protected ObjectMapper xmlMapper(String configPath, Class<?> functionClass) {
        ClassLoader modelClassLoader = resolveModelClassLoader(functionClass);
        if (configPath == null || configPath.isEmpty()) {
            // A bare `[ingest XML]` with no schema/config: use an empty XML configuration.
            return RosettaObjectMapperCreator
                    .forXML(new RosettaXMLConfiguration(Collections.emptyMap()), modelClassLoader)
                    .create();
        }
        try (InputStream inputStream = openXmlConfig(configPath, functionClass)) {
            return RosettaObjectMapperCreator.forXML(inputStream, modelClassLoader).create();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read XML configuration '" + configPath + "'", e);
        }
    }

    /**
     * Opens the XML serialization config. The classpath implementation resolves it against
     * {@link #classLoader(Class)} (falling back to the legacy Guava classpath lookup when that is
     * {@code null}); override to look it up elsewhere first — e.g. a workspace directory — before
     * delegating to {@code super}.
     */
    protected InputStream openXmlConfig(String configPath, Class<?> functionClass) throws IOException {
        ClassLoader classLoader = classLoader(functionClass);
        URL configUrl = (classLoader != null) ? classLoader.getResource(configPath) : Resources.getResource(configPath);
        if (configUrl == null) {
            throw new IllegalStateException("Could not find XML configuration '" + configPath + "' on the classpath");
        }
        return configUrl.openStream();
    }

    /**
     * The classloader against which serialization configs and model types resolve: the function
     * class's own loader, or {@link #defaultClassLoader()} when there is no function class. May return
     * {@code null}, which preserves the legacy Guava classpath-resource lookup and resolves model
     * types against this library's loader.
     */
    protected ClassLoader classLoader(Class<?> functionClass) {
        return functionClass != null ? functionClass.getClassLoader() : defaultClassLoader();
    }

    /**
     * The classloader to fall back to when a mapper is requested without a resolvable function class.
     * {@code null} here (the classpath default) preserves the legacy classpath lookup; runtimes that
     * own the model classloader (an isolated, disposable loader per model) should override this — and
     * usually only this — so function-less requests still resolve against their model.
     */
    protected ClassLoader defaultClassLoader() {
        return null;
    }

    private ClassLoader resolveModelClassLoader(Class<?> functionClass) {
        ClassLoader classLoader = classLoader(functionClass);
        return classLoader != null ? classLoader : ClasspathTransformMapperFactory.class.getClassLoader();
    }

    /**
     * Tries to resolve the {@link LabelProvider} for a {@code CSV_LABELLED} mapper from the
     * {@code @RuneLabelProvider} annotation on the function class. Returns {@code null} when none is
     * available — no function class, the class is not a {@link RosettaFunction}, or it carries no
     * {@code @RuneLabelProvider} — so the caller can fall back to unlabelled CSV.
     */
    @SuppressWarnings("unchecked")
    protected LabelProvider resolveLabelProvider(Class<?> functionClass) {
        if (functionClass == null || !RosettaFunction.class.isAssignableFrom(functionClass)) {
            return null;
        }
        return LabelProviderResolver.fromTransformFunction((Class<? extends RosettaFunction>) functionClass);
    }
}
