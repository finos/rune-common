package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;

import java.util.Collections;
import java.util.List;

public interface PostProcessor<T> {

    PostProcessor<RosettaModelObject> NO_OP_POST_PROCESSOR = (modelObject) -> Collections.emptyList();

    List<PathValue> process(T t);
}
