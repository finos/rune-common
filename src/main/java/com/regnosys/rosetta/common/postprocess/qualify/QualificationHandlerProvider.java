package com.regnosys.rosetta.common.postprocess.qualify;

import com.google.inject.ImplementedBy;

import java.util.Map;

/**
 * Provides a map of QualificationHandler, keyed by qualification root class.
 */
@ImplementedBy(EmptyQualificationHandlerProvider.class)
public interface QualificationHandlerProvider {

    Map<Class<?>, QualificationHandler<?, ?, ?>> getQualificationHandlerMap();
}
