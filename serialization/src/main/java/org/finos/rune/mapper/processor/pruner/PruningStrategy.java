package org.finos.rune.mapper.processor.pruner;

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

import com.rosetta.model.lib.RosettaModelObjectBuilder;

/**
 * A strategy interface for pruning elements from a {@link RosettaModelObjectBuilder}.
 * <p>
 * Implementations of this interface define custom pruning logic that removes
 * unnecessary or invalid elements from the builder object before finalizing the model.
 * This follows the Strategy design pattern, allowing different pruning behaviors
 * to be applied dynamically.
 * </p>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *     <li>Removing redundant keys</li>
 *     <li>Removing duplicate references</li>
 *     <li>Removing default or uninitialized values</li>
 *     <li>Eliminating redundant data</li>
 * </ul>
 *
 * @see RosettaModelObjectBuilder
 */
public interface PruningStrategy {
    void prune(RosettaModelObjectBuilder builder);
}
