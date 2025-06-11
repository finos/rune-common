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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
}
