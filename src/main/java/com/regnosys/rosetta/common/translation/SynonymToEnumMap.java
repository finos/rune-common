package com.regnosys.rosetta.common.translation;

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

import java.util.Map;
import java.util.Optional;

/**
 * Helper to access enum -> synonym -> enumValue map.
 */
public class SynonymToEnumMap {

	private final Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap;

	public SynonymToEnumMap(Map<Class<?>, Map<String, Enum<?>>> synonymToEnumMap) {
		this.synonymToEnumMap = synonymToEnumMap;
	}

	public <T extends Enum<T>> T getEnumValue(Class<T> enumClass, String valueSynonym) {
		return getEnumValueOptional(enumClass, valueSynonym).orElse(null);
	}
	public <T extends Enum<T>> Optional<T> getEnumValueOptional(Class<T> enumClass, String valueSynonym) {
		return Optional.ofNullable(synonymToEnumMap.get(enumClass)).map(x -> (T) x.get(valueSynonym));
	}
}
