package com.regnosys.rosetta.common.postprocess;

import com.regnosys.rosetta.common.postprocess.postprocessors.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;

public interface ReportBuilder {

    PostProcessorReport build(RosettaModelObject object);

}
