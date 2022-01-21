package com.regnosys.rosetta.common.translation;

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
