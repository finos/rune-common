package com.regnosys.rosetta.common.transform;

import java.util.List;

public class TestPackUtils {
    public static TestPackModel createTestPack(String dataSetName, TransformType transformType, String formattedFunctionName, List<TestPackModel.SampleModel> sampleModels) {
        return new TestPackModel(
                createTestPackId(transformType, formattedFunctionName, dataSetName),
                createPipelineId(transformType, formattedFunctionName),
                dataSetName,
                sampleModels
        );
    }

    private static String createTestPackId(TransformType transformType, String formattedFunctionName, String dataSetName) {
        return String.format("test-pack-%s-%s-%s",
                transformType.name().toLowerCase(),
                formattedFunctionName,
                dataSetName.replace(" ", "-").toLowerCase());
    }


    private static String createPipelineId(TransformType transformType, String formattedFunctionName) {
        return String.format("pipeline-%s-%s",
                transformType.name().toLowerCase(),
                formattedFunctionName);
    }

}
