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
import com.rosetta.model.lib.annotations.RuneLabelProvider;
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
import java.util.function.Supplier;

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
 * For the {@code CSV_LABELLED} format the required {@link LabelProvider} is resolved type-first: the
 * root type passed via the {@link TransformRoot} given to
 * {@link #create(TransformSerialization, Class, TransformRoot)} wins when it carries its own
 * {@code @RuneLabelProvider}, otherwise the function class's — but never on the transform's
 * {@link TransformRoot.Side#INPUT} side, where a function-rooted provider is rooted at the wrong type
 * (see {@link #resolveLabelProvider(Class, TransformRoot)}). When neither applies (e.g. a hand-written
 * function, or an input side whose type has no provider), the mapper degrades to plain (unlabelled) CSV
 * rather than failing.
 * <p>
 * A caller that supplies no {@link TransformRoot} gets the behaviour that predates root context: the
 * function's provider, unguarded. That is deliberate. The side cannot be inferred from the function's
 * annotations — a report, an enrichment and a pre-annotation model all carry a label provider and no
 * {@code @Projection} — so a factory that guessed would strip labels from exactly the transforms that
 * most need them.
 * <p>
 * <b>Extension points.</b> Only these methods are on the {@link #create} path, so overriding anything
 * else changes no constructed mapper. Per format: {@link #jsonMapper()},
 * {@link #runeJsonMapper(Class)}, {@link #csvMapper()}, {@link #csvLabelledMapper(Class, TransformRoot)}
 * and {@link #xmlMapper(String, Class)}. Below those: {@link #resolveLabelProvider(Class, TransformRoot)}
 * for label resolution, {@link #openXmlConfig(String, Class)} for the XML config lookup, and
 * {@link #classLoader(Class)} / {@link #defaultClassLoader()} for the model classloader. The two
 * deprecated single-{@code Class} overloads, {@link #csvLabelledMapper(Class)} and
 * {@link #resolveLabelProvider(Class)}, are still consulted, but only where the caller supplied no
 * {@link TransformRoot} — a supplied root outranks both.
 * <p>
 * Neither provider is deprecated, and neither is scheduled for removal — they are not in a supersession
 * relationship. A transform's output type whose labels sit entirely on nested descendants never gets a
 * type-rooted provider in any DSL version (the gate is structural: the DSL only emits one for a type
 * carrying labels on its own attributes), and a type defined in an upstream artifact may never get one
 * at all if that artifact does not carry labels on it. The function-rooted provider is what serves both
 * of those shapes, permanently, alongside the type-rooted one — deleting it would silently drop labels
 * from both.
 */
public class ClasspathTransformMapperFactory implements TransformMapperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathTransformMapperFactory.class);

    @Override
    public ObjectMapper create(TransformSerialization serialization, Class<?> functionClass) {
        return create(serialization, functionClass, (TransformRoot) null);
    }

    @Override
    public ObjectMapper create(TransformSerialization serialization, Class<?> functionClass, TransformRoot root) {
        Objects.requireNonNull(serialization, "serialization must not be null");
        switch (serialization.getFormat()) {
            case JSON:
                return jsonMapper();
            case RUNE_JSON:
                return runeJsonMapper(functionClass);
            case CSV:
                return csvMapper();
            case CSV_LABELLED:
                return csvLabelledMapper(functionClass, root);
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

    /**
     * The {@code CSV_LABELLED} mapper. Takes only what it needs — the function class and the root —
     * following {@link #xmlMapper(String, Class)}, which likewise takes the config path rather than the
     * whole {@link TransformSerialization}.
     * <p>
     * A {@code null} root means the caller declared nothing, and that state is exactly what the
     * deprecated {@link #csvLabelledMapper(Class)} has always meant. So it is delegated to rather than
     * reimplemented here: a subclass that already overrode that overload keeps deciding the case it was
     * written for, instead of compiling, running and being silently ignored.
     */
    protected ObjectMapper csvLabelledMapper(Class<?> functionClass, TransformRoot root) {
        if (root == null) {
            return csvLabelledMapper(functionClass);
        }
        LabelProvider labelProvider = resolveLabelProvider(functionClass, root);
        return csvMapperFor(labelProvider, () -> noLabelProviderWarning(functionClass, root));
    }

    /**
     * @deprecated superseded by {@link #csvLabelledMapper(Class, TransformRoot)}, which is told what
     *         sits at the root and so can resolve type-first. Kept, and still called whenever the caller
     *         supplies no {@link TransformRoot}, so a subclass that already overrides this overload
     *         keeps deciding that case — function-rooted resolution with no guard, via the equally
     *         deprecated {@link #resolveLabelProvider(Class)}. What it can no longer do is answer for a
     *         caller that did supply a root; override {@link #csvLabelledMapper(Class, TransformRoot)}
     *         to influence that.
     */
    @Deprecated
    protected ObjectMapper csvLabelledMapper(Class<?> functionClass) {
        LabelProvider labelProvider = resolveLabelProvider(functionClass);
        return csvMapperFor(labelProvider, () -> noFunctionProviderWarning(functionClass));
    }

    /**
     * The mapper for a resolved provider, or — when nothing resolved — the plain CSV mapper, having
     * logged why. The warning is built lazily because composing it is only worth doing on the path that
     * actually degrades.
     */
    private ObjectMapper csvMapperFor(LabelProvider labelProvider, Supplier<String> noProviderWarning) {
        if (labelProvider == null) {
            LOGGER.warn(noProviderWarning.get());
            return csvMapper();
        }
        return RosettaObjectMapperCreator.forCSV(labelProvider).create();
    }

    /**
     * Explains, for the WARN in {@link #csvLabelledMapper}, which of the three ways a {@code
     * CSV_LABELLED} request can end up with no provider actually happened: no provider anywhere, a root
     * type whose own hierarchy carries none, or a function provider that exists but was suppressed
     * because the caller said this is the transform's {@link TransformRoot.Side#INPUT} side (the guard
     * in {@link #resolveLabelProvider(Class, TransformRoot)}). Whichever
     * function provider would have been suppressed is named, so an ingest's missing labels are
     * self-explaining rather than a mystery.
     * <p>
     * It describes the <em>default</em> resolution, and shares
     * {@link #hasFunctionLabelProvider(Class)} with it so the two cannot drift into disagreement. A
     * subclass that overrides {@link #resolveLabelProvider(Class, TransformRoot)} and declines for
     * reasons of its own should override this reporting too.
     */
    private String noLabelProviderWarning(Class<?> functionClass, TransformRoot root) {
        boolean functionProviderSuppressed = hasFunctionLabelProvider(functionClass) && isInputSide(root);
        Class<?> rootType = root != null ? root.getType() : null;
        if (rootType != null) {
            return functionProviderSuppressed
                    ? String.format("CSV_LABELLED requested but root type %s has no @RuneLabelProvider, and "
                            + "%s's @RuneLabelProvider is rooted at its own output rather than this "
                            + "transform's input side, so it cannot be used here either; falling back to "
                            + "unlabelled CSV.", rootType.getName(), functionClass.getName())
                    : String.format("CSV_LABELLED requested but root type %s has no @RuneLabelProvider; "
                            + "falling back to unlabelled CSV.", rootType.getName());
        }
        if (functionProviderSuppressed) {
            return String.format("CSV_LABELLED requested but %s's @RuneLabelProvider is rooted at its own "
                    + "output, and this serialization is the transform's input side (no root type was "
                    + "supplied either); falling back to unlabelled CSV.", functionClass.getName());
        }
        return noFunctionProviderWarning(functionClass);
    }

    private static String noFunctionProviderWarning(Class<?> functionClass) {
        return String.format("CSV_LABELLED requested but no @RuneLabelProvider could be resolved%s; "
                + "falling back to unlabelled CSV.", functionClass != null ? " from " + functionClass.getName() : "");
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
     * The {@link LabelProvider} for a {@code CSV_LABELLED} mapper: the root type's own provider when it
     * has one, else the function's — unless the caller said this is the transform's
     * {@link TransformRoot.Side#INPUT} side, where a function-rooted provider may never be used.
     * Returns {@code null} when neither applies, so the caller degrades to unlabelled CSV.
     * <p>
     * Resolution is type-first (see the class javadoc for why): a function-rooted provider is rooted at
     * the function's <b>output</b>, so using it for any other root would resolve paths against the wrong
     * graph and could return a plausible label for a coincidentally matching path — a silent mislabel,
     * worse than no label at all. The guard is the caller's declared {@link TransformRoot.Side} and
     * nothing else. It is not inferred from {@code @Ingest}/{@code @Projection}, which cannot answer the
     * question: a report, an enrichment and a pre-annotation model each carry a label provider and no
     * {@code @Projection}, and a CSV-to-CSV transform carries both annotations with the same format.
     * <p>
     * Override this method, not the deprecated single-{@code Class} overload below, to change
     * resolution.
     */
    protected LabelProvider resolveLabelProvider(Class<?> functionClass, TransformRoot root) {
        if (root != null && root.getType() != null) {
            LabelProvider fromType = LabelProviderResolver.fromType(root.getType());
            if (fromType != null) {
                return fromType;
            }
        }
        if (isInputSide(root)) {
            return null;
        }
        // Deliberately through the deprecated overload rather than functionRootedProvider directly: a
        // subclass that already overrode it keeps influencing the one branch it was written for,
        // instead of compiling, running and being silently ignored.
        return resolveLabelProvider(functionClass);
    }

    /**
     * Whether the caller declared that the serialized graph is the transform's <b>input</b> — the one
     * state in which a function-rooted provider is provably wrong. A caller that supplied no
     * {@link TransformRoot} declared nothing, so this is {@code false} and the function's provider
     * stands, exactly as it did before root context existed.
     */
    private static boolean isInputSide(TransformRoot root) {
        return root != null && !root.isOutput();
    }

    /**
     * Resolves the {@link LabelProvider} rooted at the function class's own {@code @RuneLabelProvider}
     * annotation. Returns {@code null} when none is available — see
     * {@link #hasFunctionLabelProvider(Class)}.
     */
    @SuppressWarnings("unchecked")
    private LabelProvider functionRootedProvider(Class<?> functionClass) {
        if (!hasFunctionLabelProvider(functionClass)) {
            return null;
        }
        return LabelProviderResolver.fromTransformFunction((Class<? extends RosettaFunction>) functionClass);
    }

    /**
     * Whether a function-rooted provider exists to be resolved at all: there is a function class, it is
     * a {@link RosettaFunction}, and it carries {@code @RuneLabelProvider}. The single source of this
     * fact — both {@link #functionRootedProvider} and the WARN in {@link #noLabelProviderWarning} go
     * through it, so what the resolver does and what the log says cannot disagree.
     */
    private static boolean hasFunctionLabelProvider(Class<?> functionClass) {
        return functionClass != null
                && RosettaFunction.class.isAssignableFrom(functionClass)
                && functionClass.getAnnotation(RuneLabelProvider.class) != null;
    }

    /**
     * @deprecated superseded by {@link #resolveLabelProvider(Class, TransformRoot)}, which resolves
     *         type-first and refuses a function-rooted provider on the transform's input side.
     *         Kept, and still called on the fallback branch, so
     *         a subclass that already overrides this overload keeps deciding what it used to decide.
     *         What it can no longer do is answer for the whole of resolution: the type-rooted provider
     *         wins before this is reached, and the guard can reject the function outright. Override
     *         {@link #resolveLabelProvider(Class, TransformRoot)} to influence either.
     */
    @Deprecated
    protected LabelProvider resolveLabelProvider(Class<?> functionClass) {
        return functionRootedProvider(functionClass);
    }
}
