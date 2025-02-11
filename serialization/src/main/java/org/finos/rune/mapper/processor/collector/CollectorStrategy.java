package org.finos.rune.mapper.processor.collector;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;

public interface CollectorStrategy {
    <R extends RosettaModelObject> void collect(RosettaPath path, R instance);
}
