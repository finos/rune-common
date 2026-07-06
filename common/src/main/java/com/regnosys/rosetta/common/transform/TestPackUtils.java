package com.regnosys.rosetta.common.transform;

/*-
 * ==============
 * Rune Common
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.TransformObjectMapperFactory;
import com.regnosys.rosetta.common.serialisation.TransformMapperFactory;
import com.regnosys.rosetta.common.serialisation.TransformSerializationResolver;
import com.regnosys.rosetta.common.util.ClassPathUtils;
import com.regnosys.rosetta.common.util.DeprecationLogger;
import com.regnosys.rosetta.common.util.UrlUtils;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.transform.SerializationFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestPackUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestPackUtils.class);

    public static final Path PROJECTION_PATH = Paths.get(TransformType.PROJECTION.getResourcePath());
    public static final Path PROJECTION_CONFIG_PATH = PROJECTION_PATH.resolve("config");
    @Deprecated
    public static final Path PROJECTION_CONFIG_PATH_WITHOUT_ISO20022 = PROJECTION_CONFIG_PATH; // for backwards compatibility
    public static final Path REPORT_CONFIG_PATH = Paths.get(TransformType.REPORT.getResourcePath()).resolve("config");
    public static final Path INGEST_CONFIG_PATH = Paths.get(TransformType.TRANSLATE.getResourcePath()).resolve("config");

    public static TestPackModel createTestPack(String testPackName, TransformType transformType, String modelId, String formattedFunctionName, List<TestPackModel.SampleModel> sampleModels) {
        return new TestPackModel(createTestPackId(transformType, formattedFunctionName, testPackName), createPipelineId(transformType, modelId, formattedFunctionName), testPackName, sampleModels);
    }

    private static String createTestPackId(TransformType transformType, String formattedFunctionName, String testPackName) {
        return String.format("test-pack-%s-%s-%s", transformType.name().toLowerCase(), formattedFunctionName, testPackName.replace(" ", "-")).toLowerCase();
    }

    public static String createPipelineId(TransformType transformType, String modelId, String functionQualifiedName) {
        FunctionNameHelper functionNameHelper = new FunctionNameHelper();
        String formattedFunctionName = functionNameHelper.readableId(functionQualifiedName);
        return String.format("pipeline-%s%s-%s", transformType.name(), Optional.ofNullable(modelId).map(m -> "-" + m).orElse(""), formattedFunctionName).toLowerCase();
    }

    public static PipelineModel createPipeline(TransformType transformType,
                                               String functionQualifiedName,
                                               String displayName,
                                               String inputType,
                                               String outputType,
                                               String upstreamPipelineId,
                                               PipelineModel.Serialisation inputSerialisation,
                                               PipelineModel.Serialisation outputSerialisation,
                                               String modelId) {
        String pipelineId = createPipelineId(transformType, modelId, functionQualifiedName);
        PipelineModel.Transform transform = new PipelineModel.Transform(transformType, functionQualifiedName, inputType, outputType);
        return new PipelineModel(pipelineId, displayName, transform, upstreamPipelineId, inputSerialisation, outputSerialisation, modelId);
    }

    public static List<PipelineModel> getPipelineModels(Path resourcePath, ClassLoader classLoader, ObjectMapper jsonObjectMapper) {
        List<URL> pipelineFiles = findPaths(resourcePath, classLoader, "pipeline-.*\\.json");
        return pipelineFiles.stream()
                .map(url -> readFile(url, jsonObjectMapper, PipelineModel.class))
                .collect(Collectors.toList());
    }

    public static PipelineModel getPipelineModel(List<PipelineModel> pipelineModels, String functionName) {
        return getPipelineModel(pipelineModels, functionName, null);
    }

    public static List<PipelineModel> getPipelineModels(List<PipelineModel> pipelineModels, String functionName) {
        return pipelineModels.stream()
                .filter(p -> p.getTransform().getFunction().equals(functionName))
                .collect(Collectors.toList());
    }

    public static PipelineModel getPipelineModel(List<PipelineModel> pipelineModels, String functionName, String modelId) {
        //fallback to get the first pipeline model with the function name if pipelineId is not provided
        List<PipelineModel> pipelineModelsFunctionName = getPipelineModels(pipelineModels, functionName);
        // not found
        if (pipelineModelsFunctionName.isEmpty()) {
            throw new IllegalArgumentException(String.format("No PipelineModel found with function name %s", functionName));
        }
        // match on modelId
        return findPipelineModel(pipelineModelsFunctionName, modelId)
                // any single match
                .orElseGet(() -> getOnlyPipelineModel(pipelineModelsFunctionName).orElse(null));
    }

    private static Optional<PipelineModel> findPipelineModel(List<PipelineModel> pipelineModels, String modelId) {
        List<PipelineModel> filteredPipelineModels = pipelineModels.stream()
                .filter(p ->
                        (modelId != null && modelId.equals(p.getModelId())
                                || (modelId == null && p.getModelId() == null)))
                .collect(Collectors.toList());

        return getOnlyPipelineModel(filteredPipelineModels);
    }

    private static Optional<PipelineModel> getOnlyPipelineModel(List<PipelineModel> filteredPipelineModels) {
        if (filteredPipelineModels.size() > 1) {
            throw new IllegalArgumentException(String.format("Multiple PipelineModels found. IDs: %s", filteredPipelineModels.stream().map(PipelineModel::getId).collect(Collectors.joining(", "))));
        } else if (filteredPipelineModels.size() == 1) {
            return Optional.of(filteredPipelineModels.get(0));
        }
        return Optional.empty();
    }

    public static List<TestPackModel> getTestPackModels(Path resourcePath, ClassLoader classLoader, ObjectMapper jsonObjectMapper) {
        List<URL> testPackUrls = findPaths(resourcePath, classLoader, "test-pack-.*\\.json");
        return testPackUrls.stream()
                .map(url -> readFile(url, jsonObjectMapper, TestPackModel.class))
                .collect(Collectors.toList());
    }

    public static List<TestPackModel> getTestPackModels(List<TestPackModel> testPackModels, String pipelineId) {
        return testPackModels.stream()
                .filter(testPackModel -> testPackModel.getPipelineId() != null)
                .filter(testPackModel -> testPackModel.getPipelineId().equals(pipelineId))
                .collect(Collectors.toList());
    }

    /**
     * @deprecated this resolves the mapper from a pipeline JSON's {@code inputSerialisation}/
     *         {@code outputSerialisation}. The {@code @Ingest}/{@code @Projection} annotation on the
     *         generated function class is the source of truth — resolve it with
     *         {@link TransformSerializationResolver} and construct through a {@link TransformMapperFactory}.
     *         Kept for backward compatibility with models generated before transform annotations existed.
     */
    @Deprecated
    public static Optional<ObjectMapper> getObjectMapper(PipelineModel.Serialisation serialisation) {
        DeprecationLogger.warnOnce(LOGGER, "TestPackUtils.getObjectMapper(Serialisation)",
                "TestPackUtils.getObjectMapper(PipelineModel.Serialisation) is deprecated; resolve the "
                        + "serialization with TransformSerializationResolver and construct through a "
                        + "TransformMapperFactory.");
        return legacyObjectMapper(serialisation);
    }

    /**
     * @deprecated see {@link #getObjectMapper(PipelineModel.Serialisation)}.
     */
    @Deprecated
    public static Optional<ObjectWriter> getObjectWriter(PipelineModel.Serialisation serialisation) {
        DeprecationLogger.warnOnce(LOGGER, "TestPackUtils.getObjectWriter(Serialisation)",
                "TestPackUtils.getObjectWriter(PipelineModel.Serialisation) is deprecated; resolve the "
                        + "serialization with TransformSerializationResolver and construct through a "
                        + "TransformMapperFactory.");
        return legacyObjectMapper(serialisation).map(ObjectMapper::writerWithDefaultPrettyPrinter);
    }

    @SuppressWarnings("deprecation")
    private static Optional<ObjectMapper> legacyObjectMapper(PipelineModel.Serialisation serialisation) {
        if (serialisation == null || serialisation.getFormat() == null) {
            return Optional.empty();
        }
        if (serialisation.getFormat() == PipelineModel.Serialisation.Format.CSV_LABELLED) {
            // CSV_LABELLED additionally needs a LabelProvider, which this overload can't supply.
            // Use getObjectMapper(PipelineModel.Serialisation, LabelProvider) instead.
            throw new IllegalArgumentException(
                    "CSV_LABELLED format requires a LabelProvider resolved from the transform function. " +
                    "Use getObjectMapper(PipelineModel.Serialisation, LabelProvider) instead.");
        }
        // Delegate to the shared per-format construction. A null classloader preserves the legacy Guava
        // Resources classpath lookup historically used by this method.
        SerializationFormat format = SerializationFormat.valueOf(serialisation.getFormat().name());
        return Optional.of(TransformObjectMapperFactory.create(format, serialisation.getConfigPath(), null));
    }

    /**
     * Resolves an {@link ObjectMapper} for the given serialisation, using the supplied
     * {@link LabelProvider} for the {@code CSV_LABELLED} format.
     *
     * <p>The provider is only consulted for {@code CSV_LABELLED}; all other formats ignore it. Callers
     * resolve the provider from the (already-loaded) transform function class via
     * {@link LabelProviderResolver}, so this method does not need a {@link ClassLoader}.
     *
     * @param serialisation the output serialisation (may be {@code null})
     * @param labelProvider the provider to use for {@code CSV_LABELLED}; must be non-null for
     *                      that format
     * @return the resolved mapper, or empty when {@code serialisation} (or its format) is null
     * @throws IllegalArgumentException if the format is {@code CSV_LABELLED} but
     *                                  {@code labelProvider} is null
     * @deprecated the pipeline serialisation config is deprecated; resolve the serialization from the
     *         function's annotations with {@link TransformSerializationResolver} and construct through a
     *         {@link TransformMapperFactory} (which resolves the {@code CSV_LABELLED} labels itself).
     */
    @Deprecated
    public static Optional<ObjectMapper> getObjectMapper(PipelineModel.Serialisation serialisation, LabelProvider labelProvider) {
        DeprecationLogger.warnOnce(LOGGER, "TestPackUtils.getObjectMapper(Serialisation,LabelProvider)",
                "TestPackUtils.getObjectMapper(PipelineModel.Serialisation, LabelProvider) is deprecated; "
                        + "resolve the serialization with TransformSerializationResolver and construct through "
                        + "a TransformMapperFactory.");
        return legacyObjectMapper(serialisation, labelProvider);
    }

    /**
     * @deprecated see {@link #getObjectMapper(PipelineModel.Serialisation, LabelProvider)}.
     */
    @Deprecated
    public static Optional<ObjectWriter> getObjectWriter(PipelineModel.Serialisation serialisation, LabelProvider labelProvider) {
        DeprecationLogger.warnOnce(LOGGER, "TestPackUtils.getObjectWriter(Serialisation,LabelProvider)",
                "TestPackUtils.getObjectWriter(PipelineModel.Serialisation, LabelProvider) is deprecated; "
                        + "resolve the serialization with TransformSerializationResolver and construct through "
                        + "a TransformMapperFactory.");
        return legacyObjectMapper(serialisation, labelProvider).map(ObjectMapper::writerWithDefaultPrettyPrinter);
    }

    @SuppressWarnings("deprecation")
    private static Optional<ObjectMapper> legacyObjectMapper(PipelineModel.Serialisation serialisation, LabelProvider labelProvider) {
        if (serialisation == null || serialisation.getFormat() == null) {
            return Optional.empty();
        }
        if (serialisation.getFormat() == PipelineModel.Serialisation.Format.CSV_LABELLED) {
            if (labelProvider == null) {
                throw new IllegalArgumentException(
                        "CSV_LABELLED format requires a non-null LabelProvider resolved from the transform function.");
            }
            return Optional.of(RosettaObjectMapperCreator.forCSV(labelProvider).create());
        }
        return legacyObjectMapper(serialisation);
    }

    /**
     * Resolves the input de-serialising {@link ObjectMapper} for a transform function: the function's
     * {@code @Ingest} annotation when present, then the pipeline's (deprecated) {@code inputSerialisation},
     * and finally the supplied default mapper.
     *
     * @param inputSerialisation  the pipeline input serialisation (may be {@code null})
     * @param functionClass       the generated transform function class (may be {@code null})
     * @param defaultObjectMapper the fallback when neither an annotation nor a pipeline serialisation applies
     * @deprecated resolve the serialization with {@link TransformSerializationResolver} and construct
     *         through a {@link TransformMapperFactory}. Note this method now resolves the annotation
     *         <em>first</em> (previously the pipeline serialisation won): the annotation is generated
     *         from the model and carries the config path, so the two only disagree when a pipeline JSON
     *         was hand-edited to contradict its model.
     */
    @Deprecated
    public static ObjectMapper getInputObjectMapper(PipelineModel.Serialisation inputSerialisation,
                                                    Class<?> functionClass,
                                                    ObjectMapper defaultObjectMapper) {
        DeprecationLogger.warnOnce(LOGGER, "TestPackUtils.getInputObjectMapper",
                "TestPackUtils.getInputObjectMapper is deprecated; resolve the serialization with "
                        + "TransformSerializationResolver and construct through a TransformMapperFactory.");
        Optional<ObjectMapper> fromAnnotation = functionClass == null
                ? Optional.empty()
                : TransformObjectMapperFactory.inputForTransformFunction(functionClass, functionClass.getClassLoader());
        return fromAnnotation
                .orElseGet(() -> legacyObjectMapper(inputSerialisation).orElse(defaultObjectMapper));
    }

    /**
     * Resolves the output serialising {@link ObjectWriter} for a transform function: the function's
     * {@code @Projection} annotation when present, then the pipeline's (deprecated)
     * {@code outputSerialisation}, and finally the supplied default writer.
     *
     * @param outputSerialisation the pipeline output serialisation (may be {@code null})
     * @param functionClass       the generated transform function class (may be {@code null})
     * @param labelProvider       the provider for a {@code CSV_LABELLED} pipeline serialisation (the
     *                            annotation path resolves its own from {@code @RuneLabelProvider})
     * @param defaultObjectWriter the fallback when neither an annotation nor a pipeline serialisation applies
     * @deprecated see {@link #getInputObjectMapper(PipelineModel.Serialisation, Class, ObjectMapper)} —
     *         same replacement and the same annotation-first note.
     */
    @Deprecated
    public static ObjectWriter getOutputObjectWriter(PipelineModel.Serialisation outputSerialisation,
                                                     Class<?> functionClass,
                                                     LabelProvider labelProvider,
                                                     ObjectWriter defaultObjectWriter) {
        DeprecationLogger.warnOnce(LOGGER, "TestPackUtils.getOutputObjectWriter",
                "TestPackUtils.getOutputObjectWriter is deprecated; resolve the serialization with "
                        + "TransformSerializationResolver and construct through a TransformMapperFactory.");
        Optional<ObjectWriter> fromAnnotation = functionClass == null
                ? Optional.empty()
                : TransformObjectMapperFactory.outputForTransformFunction(functionClass, functionClass.getClassLoader())
                        .map(ObjectMapper::writerWithDefaultPrettyPrinter);
        return fromAnnotation
                .orElseGet(() -> legacyObjectMapper(outputSerialisation, labelProvider)
                        .map(ObjectMapper::writerWithDefaultPrettyPrinter)
                        .orElse(defaultObjectWriter));
    }

    public static String getReportTestPackName(String reportId) {
        return "test-pack-report-" + reportId + ".*\\.json";
    }

    public static List<URL> findPaths(Path basePath, ClassLoader classLoader, String fileName) {
        List<URL> expectations = ClassPathUtils
                .findPathsFromClassPath(Collections.singletonList(UrlUtils.toPortableString(basePath)),
                        fileName,
                        Optional.empty(),
                        classLoader)
                .stream()
                .map(UrlUtils::toUrl)
                .collect(Collectors.toList());
        return ImmutableList.copyOf(expectations);
    }

    public static <T> T readFile(URL u, ObjectMapper mapper, Class<T> clazz) {
        try (Reader src = UrlUtils.openURL(u)) {
            return mapper.readValue(src, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to read url %s", u), e);
        }
    }

    @Deprecated
    public static PipelineModel.Serialisation getSerialisation(String xmlConfigPath) {
        return xmlConfigPath == null ? null :
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.XML, xmlConfigPath);
    }

    public static PipelineModel.Serialisation getSerialisation(PipelineModel.Serialisation.Format serialisationFormat, String xmlConfigPath) {
        if (serialisationFormat == null && xmlConfigPath == null) {
            return null;
        }
        if (xmlConfigPath != null && serialisationFormat != PipelineModel.Serialisation.Format.XML) {
            throw new IllegalArgumentException("Cannot specify an xmlConfigPath and a serialisation format other than XML");
        }
        if (serialisationFormat == PipelineModel.Serialisation.Format.XML && xmlConfigPath == null) {
            throw new IllegalArgumentException("Cannot specify an XML serialisation format without an xmlXonfigPath");
        }
        return xmlConfigPath != null ? new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.XML, xmlConfigPath)
                : new PipelineModel.Serialisation(serialisationFormat, null);
    }
}
