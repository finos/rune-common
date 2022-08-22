package com.regnosys.rosetta.common.postprocess.qualify;

import java.util.Collections;
import java.util.Map;

public class EmptyQualificationHandlerProvider implements QualificationHandlerProvider {

    @Override
    public Map<Class<?>, QualificationHandler<?, ?, ?>> getQualificationHandlerMap() {
        return Collections.emptyMap();
    }
}
