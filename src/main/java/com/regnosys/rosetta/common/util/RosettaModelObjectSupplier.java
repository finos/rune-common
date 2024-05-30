package com.regnosys.rosetta.common.util;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
