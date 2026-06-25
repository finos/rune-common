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
import com.regnosys.rosetta.common.util.ClassPathUtils;
import com.regnosys.rosetta.common.util.UrlUtils;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;

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
     *         {@code outputSerialisation}. Prefer {@link TransformObjectMapperFactory}, which reads the
     *         format and config file from the {@code @Ingest}/{@code @Projection} annotation on the generated
     *         function class — making the model the single source of truth. Kept for backward compatibility.
     */
    @Deprecated
    public static Optional<ObjectMapper> getObjectMapper(PipelineModel.Serialisation serialisation) {
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
        // Delegate to the shared per-format construction in TransformObjectMapperFactory. A null classloader
        // preserves the legacy Guava Resources classpath lookup historically used by this method.
        SerializationFormat format = SerializationFormat.valueOf(serialisation.getFormat().name());
        return Optional.of(TransformObjectMapperFactory.create(format, serialisation.getConfigPath(), null));
    }

    /**
     * @deprecated see {@link #getObjectMapper(PipelineModel.Serialisation)}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Optional<ObjectWriter> getObjectWriter(PipelineModel.Serialisation serialisation) {
        return getObjectMapper(serialisation).map(ObjectMapper::writerWithDefaultPrettyPrinter);
    }

    /**
     * Resolves an {@link ObjectMapper} for the given serialisation, using the supplied
     * {@link LabelProvider} for the {@code CSV_LABELLED} format.
     *
     * <p>The provider is only consulted for {@code CSV_LABELLED}; all other formats delegate
     * to {@link #getObjectMapper(PipelineModel.Serialisation)} and ignore it. Callers resolve
     * the provider from the (already-loaded) transform function class via
     * {@link LabelProviderResolver}, so this method
     * does not need a {@link ClassLoader}.
     *
     * @param serialisation the output serialisation (may be {@code null})
     * @param labelProvider the provider to use for {@code CSV_LABELLED}; must be non-null for
     *                      that format
     * @return the resolved mapper, or empty when {@code serialisation} (or its format) is null
     * @throws IllegalArgumentException if the format is {@code CSV_LABELLED} but
     *                                  {@code labelProvider} is null
     */
    @SuppressWarnings("deprecation")
    public static Optional<ObjectMapper> getObjectMapper(PipelineModel.Serialisation serialisation, LabelProvider labelProvider) {
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
        return getObjectMapper(serialisation);
    }

    @SuppressWarnings("deprecation")
    public static Optional<ObjectWriter> getObjectWriter(PipelineModel.Serialisation serialisation, LabelProvider labelProvider) {
        return getObjectMapper(serialisation, labelProvider).map(ObjectMapper::writerWithDefaultPrettyPrinter);
    }

    /**
     * Resolves the input de-serialising {@link ObjectMapper} for a transform function, preferring the
     * pipeline's {@code inputSerialisation} when present, then the function's {@code @Ingest} annotation
     * (which a serialisation-agnostic pipeline omits, since the annotation already expresses it), and
     * finally the supplied default mapper.
     *
     * @param inputSerialisation  the pipeline input serialisation (may be {@code null})
     * @param functionClass       the generated transform function class (may be {@code null})
     * @param defaultObjectMapper the fallback when neither a pipeline serialisation nor an annotation applies
     */
    public static ObjectMapper getInputObjectMapper(PipelineModel.Serialisation inputSerialisation,
                                                    Class<?> functionClass,
                                                    ObjectMapper defaultObjectMapper) {
        return getObjectMapper(inputSerialisation)
                .orElseGet(() -> ingestObjectMapper(functionClass).orElse(defaultObjectMapper));
    }

    /**
     * Resolves the output serialising {@link ObjectWriter} for a transform function, preferring the
     * pipeline's {@code outputSerialisation} when present, then the function's {@code @Projection}
     * annotation (which a serialisation-agnostic pipeline omits, since the annotation already expresses
     * it), and finally the supplied default writer.
     *
     * @param outputSerialisation the pipeline output serialisation (may be {@code null})
     * @param functionClass       the generated transform function class (may be {@code null})
     * @param labelProvider       the provider for a {@code CSV_LABELLED} pipeline serialisation (the
     *                            annotation path resolves its own from {@code @RuneLabelProvider})
     * @param defaultObjectWriter the fallback when neither a pipeline serialisation nor an annotation applies
     */
    public static ObjectWriter getOutputObjectWriter(PipelineModel.Serialisation outputSerialisation,
                                                     Class<?> functionClass,
                                                     LabelProvider labelProvider,
                                                     ObjectWriter defaultObjectWriter) {
        return getObjectWriter(outputSerialisation, labelProvider)
                .orElseGet(() -> projectionObjectWriter(functionClass).orElse(defaultObjectWriter));
    }

    private static Optional<ObjectMapper> ingestObjectMapper(Class<?> functionClass) {
        if (functionClass == null || !functionClass.isAnnotationPresent(Ingest.class)) {
            return Optional.empty();
        }
        return Optional.of(TransformObjectMapperFactory.forTransformFunction(functionClass, functionClass.getClassLoader()));
    }

    private static Optional<ObjectWriter> projectionObjectWriter(Class<?> functionClass) {
        if (functionClass == null || !functionClass.isAnnotationPresent(Projection.class)) {
            return Optional.empty();
        }
        return Optional.of(TransformObjectMapperFactory.forTransformFunction(functionClass, functionClass.getClassLoader())
                .writerWithDefaultPrettyPrinter());
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
