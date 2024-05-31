package com.regnosys.rosetta.common.translation;

/*-
 * ==============
 * Rosetta Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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
 * ==============
 */

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MappingDelegate {

	void map(Path synonymPath, Optional<RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent);

	void map(Path synonymPath, List<? extends RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent);

	<T> void mapBasic(Path synonymPath, Optional<T> instance, RosettaModelObjectBuilder parent);

	<T> void mapBasic(Path synonymPath, Collection<? extends T> instance, RosettaModelObjectBuilder parent);

	RosettaPath getModelPath();

	List<Path> getSynonymPaths();
}
