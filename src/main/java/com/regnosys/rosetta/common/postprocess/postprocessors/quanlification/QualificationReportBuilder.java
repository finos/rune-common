package com.regnosys.rosetta.common.postprocess.postprocessors.quanlification;

import com.regnosys.rosetta.common.postprocess.ReportBuilder;
import com.regnosys.rosetta.common.postprocess.postprocessors.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;

public class QualificationReportBuilder implements ReportBuilder {

    @Override
    public PostProcessorReport build(RosettaModelObject object) {
        throw new RuntimeException("QualificationReportBuilder::build not implemented");
    }

}

