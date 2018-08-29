package com.regnosys.rosetta.common.util;

import java.util.Collections;
import java.util.List;

public interface PostProcessor<T> {

    List<PathValue> process(T t);

    static <T> PostProcessor<T> empty() {
        return x -> Collections.emptyList();
    }
}
