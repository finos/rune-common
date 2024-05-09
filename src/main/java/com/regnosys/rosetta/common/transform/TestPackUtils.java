package com.regnosys.rosetta.common.transform;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.util.ClassPathUtils;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestPackUtils {
    private static final ObjectMapper JSON_OBJECT_MAPPER = RosettaObjectMapper.getNewRosettaObjectMapper();

    private final static ObjectWriter JSON_OBJECT_WRITER =
            JSON_OBJECT_MAPPER
                    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                    .writerWithDefaultPrettyPrinter();

    public static final Path PROJECTION_PATH = Paths.get("projection");

    public static final Path PROJECTION_ISO20022_PATH = PROJECTION_PATH.resolve("iso-20022");

    public static final Path PROJECTION_OUTPUT_PATH =
            PROJECTION_ISO20022_PATH.resolve("output");

    public static final Path PROJECTION_CONFIG_PATH =
            PROJECTION_ISO20022_PATH.resolve("config");

    public static TestPackModel createTestPack(String testPackName, TransformType transformType, String formattedFunctionName, List<TestPackModel.SampleModel> sampleModels) {
        return new TestPackModel(createTestPackId(transformType, formattedFunctionName, testPackName), createPipelineId(transformType, formattedFunctionName), testPackName, sampleModels);
    }

    private static String createTestPackId(TransformType transformType, String formattedFunctionName, String testPackName) {
        return String.format("test-pack-%s-%s-%s", transformType.name().toLowerCase(), formattedFunctionName, testPackName.replace(" ", "-").toLowerCase());
    }

    private static String createPipelineId(TransformType transformType, String formattedFunctionName) {
        return String.format("pipeline-%s-%s", transformType.name().toLowerCase(), formattedFunctionName);
    }

    public static PipelineModel createPipeline(TransformType transformType, String functionQualifiedName, String displayName, String formattedFunctionName, String inputType, String outputType, String upstreamPipelineId, PipelineModel.Serialisation outputSerialisation) {
        return new PipelineModel(createPipelineId(transformType, formattedFunctionName), displayName, new PipelineModel.Transform(transformType, functionQualifiedName, inputType, outputType), upstreamPipelineId, outputSerialisation);
    }

    public static PipelineModel getPipelineModel(String functionName, ClassLoader classLoader, Path resourcePath) {
        List<URL> pipelineFiles = findPaths(resourcePath, classLoader, "pipeline-.*\\.json");
        return pipelineFiles.stream()
                .map(url -> readFile(url, JSON_OBJECT_MAPPER, PipelineModel.class))
                .filter(p -> p.getTransform().getFunction().equals(functionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No PipelineModel found with function name %s", functionName)));
    }

    public static List<TestPackModel> getTestPackModels(String pipelineId, ClassLoader classLoader, Path resourcePath) {
        List<URL> testPackUrls = findPaths(resourcePath, classLoader, "test-pack-.*\\.json");
        return testPackUrls.stream()
                .map(url -> readFile(url, JSON_OBJECT_MAPPER, TestPackModel.class))
                .filter(testPackModel -> testPackModel.getPipelineId() != null)
                .filter(testPackModel -> testPackModel.getPipelineId().equals(pipelineId))
                .collect(Collectors.toList());
    }

    public static ObjectWriter getObjectWriter(PipelineModel.Serialisation outputSerialisation) {
        if (outputSerialisation != null && outputSerialisation.getFormat() == PipelineModel.Serialisation.Format.XML) {
            URL xmlConfigPath = Resources.getResource(outputSerialisation.getConfigPath());
            try {
                return RosettaObjectMapperCreator.forXML(xmlConfigPath.openStream()).create().writerWithDefaultPrettyPrinter();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return JSON_OBJECT_WRITER;
        }
    }

    public static String getProjectionTestPackName(String reportId) {
        return "test-pack-projection-" + reportId + "-report-to-iso20022.*\\.json";
    }

    public static String getReportTestPackName(String reportId) {
        return "test-pack-report-" + reportId + ".*\\.json";
    }

    public static List<URL> findPaths(Path basePath, ClassLoader classLoader, String fileName) {
        List<URL> expectations = ClassPathUtils
                .findPathsFromClassPath(Arrays.asList(UrlUtils.toPortableString(basePath)),
                        fileName,
                        Optional.empty(),
                        classLoader)
                .stream()
                .map(UrlUtils::toUrl)
                .collect(Collectors.toList());
        return ImmutableList.copyOf(expectations);
    }

    public static <T> T readFile(URL u, ObjectMapper mapper, Class<T> clazz) {
        try {
            return mapper.readValue(UrlUtils.openURL(u), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
