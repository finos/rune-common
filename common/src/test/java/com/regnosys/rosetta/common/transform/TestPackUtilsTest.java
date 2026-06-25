package com.regnosys.rosetta.common.transform;

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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.regnosys.rosetta.common.serialisation.csv.LabelProviderResolverTest;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestPackUtilsTest {

    private static final List<PipelineModel> PIPELINE_MODELS = Arrays.asList(
            getPipelineModel("test-1-id", "func1", null),
            getPipelineModel("test-2-id", "func2", null),
            getPipelineModel("test-2-modelx-id", "func2", "modelX"),
            getPipelineModel("test-3-modely-id", "func3", "modelY"));

    private static PipelineModel getPipelineModel(String id, String functionName, String modelId) {
        return new PipelineModel(id, null, new PipelineModel.Transform(TransformType.REPORT, functionName, null, null), null, null, null, modelId);
    }

    @Test
    void shouldReturnPipelineModelWithNoModelId() {
        PipelineModel pipelineModel = TestPackUtils.getPipelineModel(PIPELINE_MODELS, "func1", null);

        assertNotNull(pipelineModel);
        assertEquals("test-1-id", pipelineModel.getId());
    }

    @Test
    void shouldReturnFallbackPipelineModelWithNoModelId() {
        PipelineModel pipelineModel = TestPackUtils.getPipelineModel(PIPELINE_MODELS, "func1", "unknownModel");

        assertNotNull(pipelineModel);
        assertEquals("test-1-id", pipelineModel.getId());
    }

    @Test
    void shouldReturnPipelineModelWithMatchingModelId() {
        PipelineModel pipelineModel = TestPackUtils.getPipelineModel(PIPELINE_MODELS, "func2", "modelX");

        assertNotNull(pipelineModel);
        assertEquals("test-2-modelx-id", pipelineModel.getId());
    }

    @Test
    void shouldReturnPipelineModelWithMatchingEmptyModelId() {
        PipelineModel pipelineModel = TestPackUtils.getPipelineModel(PIPELINE_MODELS, "func2", null);

        assertNotNull(pipelineModel);
        assertEquals("test-2-id", pipelineModel.getId());
    }

    @Test
    void shouldReturnFallbackPipelineModelWithMatchingEmptyModelId() {
        PipelineModel pipelineModel = TestPackUtils.getPipelineModel(PIPELINE_MODELS, "func3", null);

        assertNotNull(pipelineModel);
        assertEquals("test-3-modely-id", pipelineModel.getId());
    }

    @Test
    void shouldThrowExceptionForNoMatchingFunctionNames() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                TestPackUtils.getPipelineModel(PIPELINE_MODELS, "unknownFunc", null)
        );
        assertEquals("No PipelineModel found with function name unknownFunc", e.getMessage());
    }

    @Test
    void shouldThrowExceptionForMultiplePipelineModelsWithSameFunctionNameAndModelId() {
        List<PipelineModel> models = Arrays.asList(
                getPipelineModel("test-1-modelA-id", "func1", "modelA"),
                getPipelineModel("test-2-modelA-id", "func1", "modelA")
        );

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                TestPackUtils.getPipelineModel(models, "func1", "modelA")
        );
        assertEquals("Multiple PipelineModels found. IDs: test-1-modelA-id, test-2-modelA-id", e.getMessage());
    }

    @Test
    void shouldThrowExceptionForMultiplePipelineModelsWithSameFunctionNameAndNoModelId() {
        List<PipelineModel> models = Arrays.asList(
                getPipelineModel("test-1-id", "func1", null),
                getPipelineModel("test-2-id", "func1", null)
        );

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                TestPackUtils.getPipelineModel(models, "func1", null)
        );
        assertEquals("Multiple PipelineModels found. IDs: test-1-id, test-2-id", e.getMessage());
    }

    @Test
    void shouldThrowExceptionForMultiplePipelineModelsWithSameFunctionName() {
        List<PipelineModel> models = Arrays.asList(
                getPipelineModel("test-1-id", "func1", null),
                getPipelineModel("test-2-modelA-id", "func1", "modelA"),
                getPipelineModel("test-3-modelB-id", "func1", "modelB")
        );

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                TestPackUtils.getPipelineModel(models, "func1", "unknownModel")
        );
        assertEquals("Multiple PipelineModels found. IDs: test-1-id, test-2-modelA-id, test-3-modelB-id", e.getMessage());
    }

    @Test
    void shouldCreatePipelineIdWithModelId() {
        String pipelineId = TestPackUtils.createPipelineId(TransformType.REPORT, "model1", "com.example.MyReportFunction");
        assertEquals("pipeline-report-model1-my", pipelineId);
    }

    @Test
    void shouldCreatePipelineIdWithoutModelId() {
        String pipelineId = TestPackUtils.createPipelineId(TransformType.REPORT, null, "com.example.MyReportFunction");
        assertEquals("pipeline-report-my", pipelineId);
    }

    // ---------------------------------------------------------------------------
    // New tests — CSV_LABELLED pipeline wiring (#7 & #8)
    //
    // Resolution of the LabelProvider happens at the call site (from the already-loaded
    // transform function class), so TestPackUtils receives the resolved provider directly
    // via getObjectMapper/getObjectWriter(Serialisation, LabelProvider) — no ClassLoader.
    // ---------------------------------------------------------------------------

    /** Real provider for the StubCsvRow's "attr" field, resolved via @RuneLabelProvider. */
    private static LabelProvider stubLabelProvider() {
        return LabelProviderResolver.fromTransformFunction(
                LabelProviderResolverTest.StubFunctionWithProvider.class);
    }

    @Test
    void shouldThrowIllegalArgumentWhenSerialisationOnlyGetObjectMapperCalledWithCsvLabelled() {
        PipelineModel.Serialisation serialisation =
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.CSV_LABELLED, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TestPackUtils.getObjectMapper(serialisation));

        assertTrue(ex.getMessage().contains("CSV_LABELLED"),
                "Exception message should mention CSV_LABELLED");
        assertTrue(ex.getMessage().contains("getObjectMapper(PipelineModel.Serialisation, LabelProvider)"),
                "Exception message should point callers at the LabelProvider overload");
    }

    @Test
    void shouldThrowIllegalArgumentWhenSerialisationOnlyGetObjectWriterCalledWithCsvLabelled() {
        PipelineModel.Serialisation serialisation =
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.CSV_LABELLED, null);

        assertThrows(IllegalArgumentException.class,
                () -> TestPackUtils.getObjectWriter(serialisation));
    }

    @Test
    void shouldEmitLabelHeadersWhenGetObjectMapperCalledWithCsvLabelledAndProvider() throws IOException {
        PipelineModel.Serialisation serialisation =
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.CSV_LABELLED, null);

        Optional<ObjectMapper> mapperOpt = TestPackUtils.getObjectMapper(serialisation, stubLabelProvider());

        assertTrue(mapperOpt.isPresent(), "Expected a non-empty ObjectMapper for CSV_LABELLED");

        StubCsvRow row = new StubCsvRow("hello");
        String csv = mapperOpt.get().writerWithDefaultPrettyPrinter().writeValueAsString(row);

        assertTrue(csv.startsWith("My Attribute Label") || csv.startsWith("\"My Attribute Label\""),
                "Expected label header 'My Attribute Label' (possibly quoted) but got: " + csv);
        assertTrue(csv.contains("hello"),
                "Expected value 'hello' in CSV body but got: " + csv);
    }

    @Test
    void shouldThrowIllegalArgumentWhenGetObjectMapperCalledWithCsvLabelledAndNullProvider() {
        // CSV_LABELLED must never silently degrade to plain CSV — a null provider must throw.
        PipelineModel.Serialisation serialisation =
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.CSV_LABELLED, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TestPackUtils.getObjectMapper(serialisation, null));

        assertTrue(ex.getMessage().contains("CSV_LABELLED"),
                "Exception message should mention CSV_LABELLED");
        assertTrue(ex.getMessage().contains("LabelProvider"),
                "Exception message should explain a LabelProvider is required");
    }

    @Test
    void shouldEmitAttributeNameHeadersWhenGetObjectMapperCalledWithPlainCsvIgnoringProvider() throws IOException {
        // For plain CSV the provider is ignored — even when supplied, headers stay attribute names.
        PipelineModel.Serialisation serialisation =
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.CSV, null);

        Optional<ObjectMapper> mapperOpt = TestPackUtils.getObjectMapper(serialisation, stubLabelProvider());

        assertTrue(mapperOpt.isPresent());

        StubCsvRow row = new StubCsvRow("hello");
        String csv = mapperOpt.get().writerWithDefaultPrettyPrinter().writeValueAsString(row);

        assertTrue(csv.startsWith("attr"),
                "Expected attribute-name header 'attr' for plain CSV but got: " + csv);
    }

    @Test
    void shouldReturnEmptyWhenGetObjectMapperWithProviderCalledWithNullSerialisation() {
        Optional<ObjectMapper> mapperOpt = TestPackUtils.getObjectMapper(null, stubLabelProvider());

        assertFalse(mapperOpt.isPresent());
    }

    // ---------------------------------------------------------------------------
    // Minimal POJO for CSV serialisation tests
    // ---------------------------------------------------------------------------

    /** Single-field POJO whose field name matches the stub provider's labelled attribute. */
    public static class StubCsvRow {
        private final String attr;

        public StubCsvRow(String attr) {
            this.attr = attr;
        }

        public String getAttr() {
            return attr;
        }
    }

    @Ingest(format = SerializationFormat.JSON)
    private static class JsonIngestFunction implements RosettaFunction {
    }

    @Projection(format = SerializationFormat.JSON)
    private static class JsonProjectionFunction implements RosettaFunction {
    }

    private static class UnannotatedFunction implements RosettaFunction {
    }

    @Test
    void getInputObjectMapperPrefersPipelineSerialisation() {
        ObjectMapper defaultMapper = new ObjectMapper();
        PipelineModel.Serialisation jsonInput = new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.JSON, null);
        ObjectMapper resolved = TestPackUtils.getInputObjectMapper(jsonInput, UnannotatedFunction.class, defaultMapper);
        assertNotNull(resolved);
        assertNotSame(defaultMapper, resolved, "the pipeline serialisation should build its own mapper");
    }

    @Test
    void getInputObjectMapperFallsBackToIngestAnnotation() {
        ObjectMapper defaultMapper = new ObjectMapper();
        // No pipeline serialisation: the @Ingest annotation supplies the input mapper.
        ObjectMapper resolved = TestPackUtils.getInputObjectMapper(null, JsonIngestFunction.class, defaultMapper);
        assertNotNull(resolved);
        assertNotSame(defaultMapper, resolved, "the @Ingest annotation should build the mapper");
    }

    @Test
    void getInputObjectMapperFallsBackToDefaultWhenNeitherPresent() {
        ObjectMapper defaultMapper = new ObjectMapper();
        assertSame(defaultMapper, TestPackUtils.getInputObjectMapper(null, UnannotatedFunction.class, defaultMapper));
        assertSame(defaultMapper, TestPackUtils.getInputObjectMapper(null, null, defaultMapper));
        // A @Projection (not @Ingest) does not supply an input mapper.
        assertSame(defaultMapper, TestPackUtils.getInputObjectMapper(null, JsonProjectionFunction.class, defaultMapper));
    }

    @Test
    void getOutputObjectWriterFallsBackToProjectionAnnotation() {
        ObjectWriter defaultWriter = new ObjectMapper().writer();
        ObjectWriter resolved = TestPackUtils.getOutputObjectWriter(null, JsonProjectionFunction.class, null, defaultWriter);
        assertNotNull(resolved);
        assertNotSame(defaultWriter, resolved, "the @Projection annotation should build the writer");
    }

    @Test
    void getOutputObjectWriterFallsBackToDefaultWhenNeitherPresent() {
        ObjectWriter defaultWriter = new ObjectMapper().writer();
        assertSame(defaultWriter, TestPackUtils.getOutputObjectWriter(null, UnannotatedFunction.class, null, defaultWriter));
        // An @Ingest (not @Projection) does not supply an output writer.
        assertSame(defaultWriter, TestPackUtils.getOutputObjectWriter(null, JsonIngestFunction.class, null, defaultWriter));
    }
}
