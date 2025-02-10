package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.RosettaModelObjectBuilder;

public interface PruningStrategy {
    void prune(RosettaModelObjectBuilder builder);
}
