package org.finos.rune.serialization;

/*-
 * ==============
 * Rune Common
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

import com.rosetta.model.lib.RosettaModelObject;

/**
 * High-level API for serializing and deserializing objects in the Rune DSL.
 * This interface defines methods for converting objects to and from JSON format,
 * ensuring the resulting JSON aligns with the structure and semantics of the Rune DSL.
 */
public interface RuneJsonSerializer {

    /**
     * Serializes a given Rune DSL object to a JSON string.
     *
     * @param <T>        the type of the Rune DSL object, extending {@link RosettaModelObject}
     * @param runeObject the object to serialize
     * @return a JSON string representation of the given object
     */
    <T extends RosettaModelObject> String toJson(T runeObject);

    /**
     * Deserializes a JSON string into a Rune DSL object of the specified type.
     *
     * @param <T>     the type of the Rune DSL object, extending {@link RosettaModelObject}
     * @param runeJson the JSON string to deserialize
     * @param type     the class of the desired object type
     * @return an instance of the specified type populated with data from the JSON string
     */
    <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type);

}
