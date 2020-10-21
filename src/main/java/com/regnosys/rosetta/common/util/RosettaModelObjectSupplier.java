package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;

import java.util.Optional;

/**
 * Simple interface to define a RosettaModelObject look up based on type and global key.
 */
public interface RosettaModelObjectSupplier {

	/**
	 * Returns a RosettaModelObject of type T with given globalKey, if found.
	 *
	 * @param clazz RosettaModelObject class
	 * @param globalKey - global key
	 * @return returns object matching type and key
	 */
	<T extends RosettaModelObject> Optional<T> get(Class<T> clazz, String globalKey);
}
