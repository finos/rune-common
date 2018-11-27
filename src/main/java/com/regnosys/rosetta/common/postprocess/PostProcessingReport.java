package com.regnosys.rosetta.common.postprocess;

import com.regnosys.rosetta.common.postprocess.postprocessors.PostProcessorReport;

import java.util.Map;
import java.util.Optional;

public class PostProcessingReport {

    Map<Class<? extends PostProcessor>, PostProcessorReport> reports;

    public PostProcessingReport(Map<Class<? extends PostProcessor>, PostProcessorReport> reports) {
        this.reports = reports;
    }

    public Optional<PostProcessorReport> forPostProcessor(Class<? extends PostProcessor> processor) {
        return Optional.ofNullable(reports.get(processor));
    }
}
