package com.regnosys.rosetta.common.postprocess.qualify;

import java.util.Collections;
import java.util.Map;

public class EmptyQualificationConfigProvider implements QualificationConfigProvider {

    @Override
    public Map<Class<?>, QualificationConfig<?, ?, ?>> getQualificationConfig() {
        return Collections.emptyMap();
    }
}
