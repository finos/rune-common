package com.regnosys.rosetta.common.ingestor.postprocess;

import com.rosetta.model.lib.RosettaModelObject;

import java.util.Optional;

/**
 * A function that takes a {@link RosettaModelObject}, operates on that object and returns a new instance of that object.
 */
public interface PostProcessor {

    <T extends RosettaModelObject> T process(Class<T> rosettaType, T instance);

    Optional<PostProcessorReport> getReport();

    static PostProcessor identity() {
        return new PostProcessor() {
            @Override
            public <T extends RosettaModelObject> T process(Class<T> rosettaType, T modelObject) {
                return modelObject;
            }

            @Override
            public Optional<PostProcessorReport> getReport() {
                return Optional.empty();
            }
        };
    }

}
