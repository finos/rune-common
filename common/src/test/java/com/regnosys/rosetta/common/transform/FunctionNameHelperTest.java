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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionNameHelperTest {

    private FunctionNameHelper functionNameHelper;
    
    @BeforeEach
    void setUp() {
        functionNameHelper = new FunctionNameHelper();
    }

    @Test
    void getInputType() {
        assertEquals("com.rosetta.model.lib.RosettaModelObject", functionNameHelper.getInputType(PipelineTestUtils.Enrich_Type_1ToType_2.class));
    }

    @Test
    void getOutputType() {
        assertEquals("java.lang.String", functionNameHelper.getOutputType(PipelineTestUtils.Enrich_Type_1ToType_2.class));
    }

    @Test
    void getFuncMethod() {
        assertEquals("evaluate", functionNameHelper.getFuncMethod(PipelineTestUtils.Enrich_Type_1ToType_2.class).getName());
    }

    @Test
    void getInputTypeNoEvaluate() {
        assertThrows(EvaluateFunctionNotFoundException.class, () -> functionNameHelper.getInputType(PipelineTestUtils.Report_Type_2ToType_3.class));
    }

    @Test
    void getOutputTypeNoEvaluate() {
        assertThrows(EvaluateFunctionNotFoundException.class, () -> functionNameHelper.getOutputType(PipelineTestUtils.Report_Type_2ToType_3.class));
    }

    @Test
    void getFuncMethodNoEvaluate() {
        assertThrows(EvaluateFunctionNotFoundException.class, () -> functionNameHelper.getFuncMethod(PipelineTestUtils.Report_Type_2ToType_3.class));
    }

    @Test
    void getNameEnrich() {
        assertEquals("Type1 To Type2", functionNameHelper.getName(PipelineTestUtils.Enrich_Type_1ToType_2.class));
    }

    @Test
    void getNameReport() {
        assertEquals("Type2 To Type3", functionNameHelper.getName(PipelineTestUtils.Report_Type_2ToType_3.class));
    }

    @Test
    void getNameProjection() {
        assertEquals("Type3 To Type4", functionNameHelper.getName(PipelineTestUtils.Project_Type_3ToType_4.class));
    }

    @Test
    void readableIdEnrich() {
        assertEquals("type1-to-type2", functionNameHelper.readableId(PipelineTestUtils.Enrich_Type_1ToType_2.class));
    }

    @Test
    void readableIdReportFromClass() {
        assertEquals("type2-to-type3", functionNameHelper.readableId(PipelineTestUtils.Report_Type_2ToType_3.class));
    }

    @Test
    void readableIdReportFromString() {
        PipelineModel.Transform transform1 = new PipelineModel.Transform(TransformType.REPORT, "com.example.REGTradeReportFunction", "inputType", "outputType");
        assertEquals("reg-trade-report", functionNameHelper.readableId(transform1.getFunction()));
        
        PipelineModel.Transform transform2 = new PipelineModel.Transform(TransformType.REPORT, "REGTradeReportFunction", "inputType", "outputType");
        assertEquals("reg-trade-report", functionNameHelper.readableId(transform2.getFunction()));
    }

    @Test
    void readableIdProjectionFromClass() {
        assertEquals("type3-to-type4", functionNameHelper.readableId(PipelineTestUtils.Project_Type_3ToType_4.class));
    }

    @Test
    void readableIdProjectionFromString() {
        PipelineModel.Transform transform1 = new PipelineModel.Transform(TransformType.PROJECTION, "Project_REGTradeTwoReportToIso20022", "inputType", "outputType");
        assertEquals("reg-trade-two-report-to-iso20022", functionNameHelper.readableId(transform1.getFunction()));
        
        PipelineModel.Transform transform2 = new PipelineModel.Transform(TransformType.PROJECTION, "com.example.Project_REGTradeTwoReportToIso20022", "inputType", "outputType");
        assertEquals("reg-trade-two-report-to-iso20022", functionNameHelper.readableId(transform2.getFunction()));
    }

    @Test
    void readableIdAllUpperCase() {
        assertEquals("type2-to-type3", functionNameHelper.readableId(PipelineTestUtils.Report_TYPE_2_TO_TYPE_3.class));
    }

    @Test
    void nameAllUpperCase() {
        assertEquals("Type2 To Type3", functionNameHelper.getName(PipelineTestUtils.Report_TYPE_2_TO_TYPE_3.class));
    }

    @Test
    void getNameFromId() {
        assertEquals("Test Pack id", functionNameHelper.capitalizeFirstLetter("test Pack id"));
    }

    @Test
    void getNameFromIdAllCaps() {
        assertEquals("TESTPACKID", functionNameHelper.capitalizeFirstLetter("TESTPACKID"));
    }

    @Test
    void getNameFromIdAllLowerCase() {
        assertEquals("Test-pack-id", functionNameHelper.capitalizeFirstLetter("test-pack-id"));
    }

    @Test
    void getReadableFunctionName() {
        assertEquals("Type Name", functionNameHelper.readableFunctionName("Project_TypeName"));
    }
}
