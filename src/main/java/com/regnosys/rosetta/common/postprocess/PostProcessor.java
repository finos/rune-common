package com.regnosys.rosetta.common.postprocess;

import com.regnosys.rosetta.common.postprocess.postprocessors.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;

import java.util.Optional;

public interface PostProcessor {

    RosettaModelObject process(RosettaModelObject t);

    Optional<PostProcessorReport> getReport();

    static PostProcessor empty() {
        return new PostProcessor() {
            @Override
            public RosettaModelObject process(RosettaModelObject t) {
                return t;
            }

            @Override
            public Optional<PostProcessorReport> getReport() {
                return Optional.empty();
            }
        };
    }

}
