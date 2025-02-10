package org.finos.rune.mapper.processor.collector;

import com.rosetta.model.lib.RosettaModelObject;

public interface CollectorStrategy {
    <R extends RosettaModelObject> void collect(R instance);
}
