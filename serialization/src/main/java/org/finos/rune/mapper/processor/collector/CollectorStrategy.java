package org.finos.rune.mapper.processor.collector;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import com.rosetta.model.lib.RosettaModelObject;

/**
 * A strategy interface for collecting and introspecting data from a {@link RosettaModelObject}.
 * <p>
 * Implementations of this interface define custom logic to traverse a given model object
 * and extract relevant information. This follows the Strategy design pattern, allowing
 * different collection behaviors to be applied dynamically based on the use case.
 * </p>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *     <li>Collecting information about keys and references in the model</li>
 *     <li>Gathering specific data points from the model</li>
 *     <li>Validating or analyzing object structures</li>
 *     <li>Building summaries or reports based on model contents</li>
 * </ul>
 *
 * @see RosettaModelObject
 */
public interface CollectorStrategy {
    <R extends RosettaModelObject> void collect(R instance);
}
