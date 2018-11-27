package com.regnosys.rosetta.common.postprocess;

import com.rosetta.model.lib.RosettaModelObject;

public interface PostProcessorRunner {

    <T extends RosettaModelObject> T postProcess(Class<T> rosettaModelClass, T instance);

    PostProcessingReport buildProcessingReport();

}
