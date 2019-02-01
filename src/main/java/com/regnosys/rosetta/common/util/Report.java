package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;

public class Report<T extends RosettaModelObject> {

	final T rosettaModelInstance;

	public Report(T rosettaModelInstance) {
		this.rosettaModelInstance = rosettaModelInstance;
	}

	public T getRosettaModelInstance() {
		return rosettaModelInstance;
	}
}
