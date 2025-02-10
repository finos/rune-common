package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.RosettaModelObjectBuilder;

public class EmptyAttributePruningStrategy implements PruningStrategy {
    @Override
    public void prune(RosettaModelObjectBuilder builder) {
        builder.prune();
    }
}
