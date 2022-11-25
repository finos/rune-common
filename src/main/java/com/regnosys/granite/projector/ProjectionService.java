package com.regnosys.granite.projector;

import com.rosetta.model.lib.RosettaModelObject;

public interface ProjectionService<R extends RosettaModelObject, T> {

	ProjectionReport<R, T> project(R rosettaModelInstance, Class<T> projectedClass);
}
