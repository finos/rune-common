package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;

import java.util.Optional;

public interface RosettaModelObjectSupplier {

	<T extends RosettaModelObject> Optional<T> get(Class<T> clazz, String globalKey);
}
