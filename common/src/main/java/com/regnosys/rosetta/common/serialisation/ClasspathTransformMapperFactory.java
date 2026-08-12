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
import com.regnosys.rosetta.common.serialisation.csv.config.HeaderStyle;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
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
 * <b>Which CSV mapper a CSV format gets — the configuration decides, not the format.</b>
 * <ul>
 *   <li>{@code CSV} reads its {@link RosettaCSVConfiguration} and is labelled or unlabelled according to
 *       it: {@code headerStyle=LABEL} resolves a {@link LabelProvider}, anything else resolves none —
 *       <b>even for a function carrying {@code @RuneLabelProvider}</b>. A {@code LABEL} configuration for
 *       which no provider can be found fails rather than degrading; see
 *       {@link #csvMapper(String, Class, TransformRoot)}.</li>
 *   <li>{@code CSV_LABELLED} is <b>frozen</b>: it reads no configuration, always resolves a provider, and
 *       degrades to plain CSV when none is found. Any {@code configPath} it declares is dropped, with a
 *       WARN naming the fix. The format is on its way out; {@code CSV} with
 *       {@code "headerStyle": "LABEL"} is where a transform using it goes — same label resolution, plus
 *       the dialect, list delimiter, null token and header settings.</li>
 * </ul>
 * <p>
 * Wherever a {@link LabelProvider} is required — by either format — it is resolved type-first: the
 * root type passed via the {@link TransformRoot} given to
 * {@link #create(TransformSerialization, Class, TransformRoot)} wins when it carries its own
 * {@code @RuneLabelProvider}, otherwise the function class's — but never on the transform's
 * {@link TransformRoot.Side#INPUT} side, where a function-rooted provider is rooted at the wrong type
 * (see {@link #resolveLabelProvider(Class, TransformRoot)}). When neither applies, a {@code CSV_LABELLED}
 * mapper degrades to plain (unlabelled) CSV and a {@code CSV} mapper whose configuration asked for labels
 * fails.
 * <p>
 * A caller that supplies no {@link TransformRoot} gets the function's provider, unguarded, as it did
 * before root context existed. The side cannot be inferred from the function's annotations — a report, an
 * enrichment and a pre-annotation model all carry a label provider and no {@code @Projection} — so a
 * factory that guessed would strip labels from exactly the transforms that most need them.
 * <p>
 * Both provider kinds are permanent: a type whose labels sit entirely on nested descendants never gets a
 * type-rooted provider (the DSL only emits one for a type carrying labels on its own attributes), and the
 * function-rooted provider is what serves that shape.
 * <p>
 * <b>Extension points.</b> Only these methods are on the {@link #create} path, so overriding anything
 * else changes no constructed mapper. Per format: {@link #jsonMapper()},
 * {@link #runeJsonMapper(Class)}, {@link #csvMapper(String, Class, TransformRoot)},
 * {@link #csvLabelledMapper(Class, TransformRoot)} and {@link #xmlMapper(String, Class)}. Below
 * those: {@link #resolveLabelProvider(Class, TransformRoot)} for label resolution,
 * {@link #openXmlConfig(String, Class)} / {@link #openCsvConfig(String, Class)} for the config lookups,
 * and {@link #classLoader(Class)} / {@link #defaultClassLoader()} for the model classloader.
 * <p>
 * Three narrower overloads predate the parameters above and are <b>deprecated since 12.10.0, for removal
 * in the next major version</b>. Each is still consulted, but only for the case it was written for, so an
 * existing override keeps deciding that case rather than being silently ignored:
 * <ul>
 *   <li>{@link #csvLabelledMapper(Class)} and {@link #resolveLabelProvider(Class)} — only where the
 *       caller supplied no {@link TransformRoot}. A supplied root outranks both.</li>
 *   <li>{@link #csvMapper()} — only where the transform declares no {@code configPath}. A declared
 *       config path outranks it. It is also where both {@code CSV_LABELLED} paths degrade to.</li>
 * </ul>
 * Prefer the wider forms in new code and new overrides.
 * <p>
 * <b>Supplying a CSV configuration at deployment time.</b> A deployment that must change the CSV dialect
 * without rebuilding the model overrides {@link #openCsvConfig(String, Class)} and returns its own
 * document, exactly as it would override {@link #openXmlConfig(String, Class)} for an XML config in a
 * workspace directory. Since the configuration decides the header style, such an override can also make a
 * {@code CSV} transform labelled — or unlabelled — provided a provider resolves for the {@code LABEL}
 * case. Precedence, highest first:
 * <ol>
 *   <li>whatever {@link #openCsvConfig(String, Class)} returns — the override, if there is one, else the
 *       config packaged on the model classpath at the declared {@code configPath};</li>
 *   <li>{@code RosettaCSVConfiguration.EMPTY} (RFC 4180, attribute-name headers) when the transform
 *       declares no {@code configPath}.</li>
 * </ol>
 * Neither step applies to {@code CSV_LABELLED}, which reads no configuration.
 * <p>
 * The hook is only consulted when the transform declares a {@code configPath}, since the path is what
 * keys the lookup. A bare {@code [ingest CSV]} gets {@code EMPTY} and a deployment cannot override it, so
 * a model whose feed needs a non-default dialect must declare a {@code configPath} even if the file it
 * names is replaced at deployment time. A deployment chooses the <em>content</em> behind a path; it
 * cannot introduce a configuration where the model asked for none.
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
                return csvMapper(serialization.getConfigPath(), functionClass, root);
            case CSV_LABELLED:
                warnIfConfigPathDropped(serialization.getConfigPath(), functionClass);
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

    /**
     * The {@code CSV} mapper — <b>labelled or not according to its own configuration</b>. Mirrors
     * {@link #xmlMapper(String, Class)} in resolving the config: no config path means a bare
     * {@code [ingest CSV]} with no schema/config, so an empty CSV configuration is used; otherwise the
     * config is read via {@link #openCsvConfig(String, Class)}.
     * <p>
     * {@code headerStyle=LABEL} resolves a {@link LabelProvider} — type-first, with the input-side guard,
     * through the same {@link #resolveLabelProvider(Class, TransformRoot)} {@code CSV_LABELLED} uses — and
     * any other header style resolves none, <b>even for a function carrying {@code @RuneLabelProvider}</b>.
     * That is why this takes the {@link TransformRoot}: only the caller knows which side of the transform
     * is being serialized, and a function-rooted provider may not be used on the input side.
     * <p>
     * A {@code LABEL} configuration for which no provider resolves <b>fails</b>, where {@code CSV_LABELLED}
     * degrades to plain CSV. {@code CSV_LABELLED}'s degradation is a compatibility obligation to callers
     * that predate configuration; a config declaring {@code headerStyle=LABEL} is an explicit request, and
     * the only way to proceed would be to treat it as {@code ATTRIBUTE_NAME} — writing a well-formed file
     * with the wrong header row.
     * <p>
     * The no-config-path case is delegated to the deprecated no-argument {@link #csvMapper()} rather than
     * reimplemented, so a subclass that already overrides that released extension point keeps deciding the
     * case it was written for. Nothing is dropped on that path: it is reached only when there is no
     * configuration, and so none that could have asked for labels.
     *
     * @throws IllegalArgumentException if the configuration declares {@code headerStyle=LABEL} and no
     *                                  {@link LabelProvider} can be resolved
     */
    protected ObjectMapper csvMapper(String configPath, Class<?> functionClass, TransformRoot root) {
        if (configPath == null || configPath.isEmpty()) {
            return csvMapper();
        }
        RosettaCSVConfiguration configuration = loadCsvConfig(configPath, functionClass);
        if (configuration.getHeaderStyle() != HeaderStyle.LABEL) {
            return RosettaObjectMapperCreator.forCSV(configuration).create();
        }
        LabelProvider labelProvider = resolveLabelProvider(functionClass, root);
        if (labelProvider == null) {
            throw new IllegalArgumentException(unhonourableLabelConfigMessage(configPath, functionClass, root));
        }
        return RosettaObjectMapperCreator.forCSV(configuration, labelProvider).create();
    }

    /**
     * @deprecated since 12.10.0, will be removed in the next major version. Superseded by
     *         {@link #csvMapper(String, Class, TransformRoot)}, which is told the transform's
     *         {@code configPath} and so can honour a CSV configuration. Still called whenever the transform
     *         declares no config path — the RFC 4180 defaults, with no configuration read — and where both
     *         {@code CSV_LABELLED} paths degrade to when no {@link LabelProvider} resolves. It can no
     *         longer answer for a transform that did declare a config path; override
     *         {@link #csvMapper(String, Class, TransformRoot)} to influence that.
     *         <p>
     *         Deprecated with javadoc rather than {@code @Deprecated(forRemoval = true, since = "...")}
     *         because this module compiles at {@code maven.compiler.release=8} and those attributes are
     *         Java 9+. The same applies to {@link #csvLabelledMapper(Class)}.
     */
    @Deprecated
    protected ObjectMapper csvMapper() {
        return RosettaObjectMapperCreator.forCSV().create();
    }

    /**
     * The {@code CSV_LABELLED} mapper: the format's own frozen behaviour, and <b>no configuration</b>.
     * The provider is resolved type-first with the input-side guard, and the mapper degrades to plain CSV
     * when none resolves.
     * <p>
     * <b>It reads no {@link RosettaCSVConfiguration}, deliberately, and takes no config path.</b> This
     * format is on its way out — see the class javadoc — and a configurable CSV transform is spelled
     * {@code format = CSV} with {@code headerStyle: LABEL}, which reaches
     * {@link #csvMapper(String, Class, TransformRoot)} and resolves a provider by the same rules. Freezing
     * it also keeps out a contradiction only this format could express: a transform that resolves a
     * provider and then declares a header style that never consults one. A declared {@code configPath} is
     * reported dropped before this is reached — see {@link #warnIfConfigPathDropped}.
     * <p>
     * A {@code null} root means the caller declared nothing, which is exactly what the deprecated
     * {@link #csvLabelledMapper(Class)} has always meant, so that overload is delegated to rather than
     * reimplemented here.
     */
    protected ObjectMapper csvLabelledMapper(Class<?> functionClass, TransformRoot root) {
        if (root == null) {
            return csvLabelledMapper(functionClass);
        }
        LabelProvider labelProvider = resolveLabelProvider(functionClass, root);
        return labelledOrPlainCsvMapper(labelProvider, () -> noLabelProviderWarning(functionClass, root));
    }

    /**
     * @deprecated since 12.10.0, will be removed in the next major version. Superseded by
     *         {@link #csvLabelledMapper(Class, TransformRoot)}, which is told what sits at the root and so
     *         can resolve type-first. Still called whenever the caller supplies no {@link TransformRoot} —
     *         function-rooted resolution with no guard, via the equally deprecated
     *         {@link #resolveLabelProvider(Class)}. It can no longer answer for a caller that did supply a
     *         root; override {@link #csvLabelledMapper(Class, TransformRoot)} to influence that.
     */
    @Deprecated
    protected ObjectMapper csvLabelledMapper(Class<?> functionClass) {
        LabelProvider labelProvider = resolveLabelProvider(functionClass);
        return labelledOrPlainCsvMapper(labelProvider, () -> noFunctionProviderWarning(functionClass));
    }

    /**
     * The {@code CSV_LABELLED} mapper for a resolved provider, or — when nothing resolved — the plain CSV
     * mapper, having logged why. The warning is built lazily since only the degrading path needs it.
     * <p>
     * A resolved provider implies {@code headerStyle=LABEL}, which is what
     * {@code RosettaObjectMapperCreator.forCSV(LabelProvider)} derives. The degrade goes through the
     * deprecated {@link #csvMapper()} hook, where a configuration-free plain CSV mapper has always come
     * from on this path.
     */
    private ObjectMapper labelledOrPlainCsvMapper(LabelProvider labelProvider, Supplier<String> noProviderWarning) {
        if (labelProvider == null) {
            LOGGER.warn(noProviderWarning.get());
            return csvMapper();
        }
        return RosettaObjectMapperCreator.forCSV(labelProvider).create();
    }

    /**
     * Reads the CSV configuration a transform declares, through the {@link #openCsvConfig(String, Class)}
     * hook. Only called with a non-empty {@code configPath}: a transform declaring none takes
     * {@code RosettaCSVConfiguration.EMPTY} without a lookup, mirroring
     * {@link #xmlMapper(String, Class)}'s "no config path" branch.
     */
    private RosettaCSVConfiguration loadCsvConfig(String configPath, Class<?> functionClass) {
        try (InputStream inputStream = openCsvConfig(configPath, functionClass)) {
            return RosettaCSVConfiguration.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV configuration '" + configPath + "'", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "CSV configuration '" + configPath + "' is not a valid configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Which of the three ways a request for labels can end up with no provider actually happened: no
     * provider anywhere, a root type whose own hierarchy carries none, or a function provider suppressed
     * because the caller said this is the transform's {@link TransformRoot.Side#INPUT} side (the guard in
     * {@link #resolveLabelProvider(Class, TransformRoot)}). A suppressed function provider is named, so an
     * ingest's missing labels explain themselves.
     * <p>
     * The <b>diagnosis only</b>, with no consequence clause, since the callers differ in the consequence:
     * {@code CSV_LABELLED} degrades and warns ({@link #noLabelProviderWarning}), while a {@code CSV}
     * configuration declaring {@code headerStyle=LABEL} fails
     * ({@link #unhonourableLabelConfigMessage}).
     * <p>
     * It describes the <em>default</em> resolution, and shares {@link #hasFunctionLabelProvider(Class)}
     * with it so the reporting and the resolver cannot disagree. A subclass that overrides
     * {@link #resolveLabelProvider(Class, TransformRoot)} and declines for reasons of its own should
     * override this reporting too.
     */
    private String noProviderReason(Class<?> functionClass, TransformRoot root) {
        boolean functionProviderSuppressed = hasFunctionLabelProvider(functionClass) && isInputSide(root);
        Class<?> rootType = root != null ? root.getType() : null;
        if (rootType != null) {
            return functionProviderSuppressed
                    ? String.format("root type %s has no @RuneLabelProvider, and %s's @RuneLabelProvider is "
                            + "rooted at its own output rather than this transform's input side, so it cannot "
                            + "be used here either", rootType.getName(), functionClass.getName())
                    : String.format("root type %s has no @RuneLabelProvider", rootType.getName());
        }
        if (functionProviderSuppressed) {
            return String.format("%s's @RuneLabelProvider is rooted at its own output, and this serialization "
                    + "is the transform's input side (no root type was supplied either)", functionClass.getName());
        }
        return noProviderAnywhereReason(functionClass);
    }

    /**
     * The terminal case of {@link #noProviderReason}: nothing carries a provider. Separate because the
     * deprecated {@link #csvLabelledMapper(Class)} path reaches it with no {@link TransformRoot} to
     * diagnose.
     */
    private static String noProviderAnywhereReason(Class<?> functionClass) {
        return String.format("no @RuneLabelProvider could be resolved%s",
                functionClass != null ? " from " + functionClass.getName() : "");
    }

    private String noLabelProviderWarning(Class<?> functionClass, TransformRoot root) {
        return "CSV_LABELLED requested but " + noProviderReason(functionClass, root)
                + "; falling back to unlabelled CSV.";
    }

    private static String noFunctionProviderWarning(Class<?> functionClass) {
        return "CSV_LABELLED requested but " + noProviderAnywhereReason(functionClass)
                + "; falling back to unlabelled CSV.";
    }

    /**
     * Explains that a CSV configuration asked for label headers and no {@link LabelProvider} could be
     * found to produce them. Names both ways out, since either side can be the one that is wrong: a model
     * missing a {@code @RuneLabelProvider}, or a configuration asking for labels it did not mean to.
     */
    private String unhonourableLabelConfigMessage(String configPath, Class<?> functionClass, TransformRoot root) {
        return String.format("CSV configuration '%s' declares headerStyle=LABEL, but %s. A LABEL header style "
                        + "has no attribute-to-label mapping without a LabelProvider, and treating it as "
                        + "ATTRIBUTE_NAME would silently discard the header style the configuration asks for. "
                        + "Either annotate the serialized root type with @RuneLabelProvider (and pass it as the "
                        + "TransformRoot), or set headerStyle=ATTRIBUTE_NAME in '%s'.",
                configPath, noProviderReason(functionClass, root), configPath);
    }

    /**
     * Reports a {@code configPath} declared by a {@code CSV_LABELLED} transform, which reads no
     * configuration at all. Dropped in silence, such a transform serialises with default RFC 4180
     * punctuation and gives no indication that its own configuration was never read. The message says what
     * the file will look like instead and names the fix (move to the {@code CSV} format). A WARN rather
     * than an exception, because a {@code CSV_LABELLED} transform must keep working.
     */
    private static void warnIfConfigPathDropped(String configPath, Class<?> functionClass) {
        if (configPath == null || configPath.isEmpty()) {
            return;
        }
        LOGGER.warn("CSV_LABELLED transform {} declares configPath '{}', but the CSV_LABELLED format reads no "
                        + "configuration: it was NOT applied and default RFC 4180 punctuation is in use. Declare "
                        + "format = CSV instead, with \"headerStyle\": \"LABEL\" in '{}' — the CSV format honours "
                        + "the whole configuration and resolves the label provider by the same rules.",
                functionClass != null ? functionClass.getName() : "(unknown function)", configPath, configPath);
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
     * Opens the CSV serialization config, for the {@code CSV} format. The classpath implementation resolves
     * it against {@link #classLoader(Class)} (falling back to the legacy Guava classpath lookup when that
     * is {@code null}), exactly like {@link #openXmlConfig(String, Class)}. This is the hook a runtime that
     * keeps its CSV configuration elsewhere overrides, and the one route by which a deployment supplies its
     * own — see the class javadoc for the precedence order.
     * <p>
     * <b>Only consulted when the transform declares a {@code configPath}</b>, since the path is what keys
     * the lookup. An override may ignore {@code configPath} and return the same document for every path,
     * but it cannot introduce a configuration for a transform that declares none: that request never
     * reaches here and takes {@code RosettaCSVConfiguration.EMPTY}.
     * <p>
     * <b>Never consulted for {@code CSV_LABELLED}</b>, which reads no configuration; a path declared by
     * such a transform is reported dropped instead.
     */
    protected InputStream openCsvConfig(String configPath, Class<?> functionClass) throws IOException {
        ClassLoader classLoader = classLoader(functionClass);
        URL configUrl = (classLoader != null) ? classLoader.getResource(configPath) : Resources.getResource(configPath);
        if (configUrl == null) {
            throw new IllegalStateException("Could not find CSV configuration '" + configPath + "' on the classpath");
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
     * Resolution is type-first: a function-rooted provider is rooted at the function's <b>output</b>, so
     * using it for any other root resolves paths against the wrong graph and could return a plausible label
     * for a coincidentally matching path — a silent mislabel, worse than no label. The guard is the
     * caller's declared {@link TransformRoot.Side} and nothing else; it is not inferred from
     * {@code @Ingest}/{@code @Projection}, which cannot answer the question — a report, an enrichment and a
     * pre-annotation model each carry a label provider and no {@code @Projection}, and a CSV-to-CSV
     * transform carries both annotations with the same format.
     * <p>
     * Override this method, not the deprecated single-{@code Class} overload below, to change resolution.
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
        // Through the deprecated overload rather than functionRootedProvider directly, so a subclass that
        // already overrode it keeps influencing the one branch it was written for.
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
     * Whether a function-rooted provider exists to be resolved at all: there is a function class, it is a
     * {@link RosettaFunction}, and it carries {@code @RuneLabelProvider}. Both
     * {@link #functionRootedProvider} and the WARN in {@link #noLabelProviderWarning} go through here, so
     * what the resolver does and what the log says cannot disagree.
     */
    private static boolean hasFunctionLabelProvider(Class<?> functionClass) {
        return functionClass != null
                && RosettaFunction.class.isAssignableFrom(functionClass)
                && functionClass.getAnnotation(RuneLabelProvider.class) != null;
    }

    /**
     * @deprecated since 12.10.0, will be removed in the next major version. Superseded by
     *         {@link #resolveLabelProvider(Class, TransformRoot)}, which resolves type-first and refuses a
     *         function-rooted provider on the transform's input side. Still called on the fallback branch,
     *         but it no longer answers for the whole of resolution: the type-rooted provider wins before
     *         this is reached, and the guard can reject the function outright. Override
     *         {@link #resolveLabelProvider(Class, TransformRoot)} to influence either.
     */
    @Deprecated
    protected LabelProvider resolveLabelProvider(Class<?> functionClass) {
        return functionRootedProvider(functionClass);
    }
}
