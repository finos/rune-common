package com.regnosys.rosetta.common.postprocess.qualify;

import com.google.inject.ImplementedBy;

import java.util.Map;

/**
 * Provides a map of QualificationConfig that is keyed by qualification root class.
 */
@ImplementedBy(EmptyQualificationConfigProvider.class)
public interface QualificationConfigProvider {

    Map<Class<?>, QualificationConfig<?, ?, ?>> getQualificationConfig();
}
