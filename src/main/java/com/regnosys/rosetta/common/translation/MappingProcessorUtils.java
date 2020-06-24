package com.regnosys.rosetta.common.translation;

import com.rosetta.model.lib.path.RosettaPath;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.util.PathUtils.toPath;
import static com.regnosys.rosetta.common.util.PathUtils.toRosettaPath;

public class MappingProcessorUtils {

	public static void setValueAndUpdateMappings(Path synonymPath, Consumer<String> setter, List<Mapping> mappings, RosettaPath rosettaPath) {
		List<Mapping> mappingsFromSynonymPath = filterMappings(mappings, synonymPath);
		getNonNullMappedValue(mappingsFromSynonymPath).ifPresent(value -> {
			// set value on model
			setter.accept(value);
			// update mappings
			mappingsFromSynonymPath.forEach(m -> updateMappingSuccess(m, rosettaPath));
		});
	}

	public static void setValueAndOptionallyUpdateMappings(Path synonymPath, Function<String, Boolean> func, List<Mapping> mappings, RosettaPath rosettaPath) {
		List<Mapping> mappingsFromSynonymPath = filterMappings(mappings, synonymPath);
		getNonNullMappedValue(mappingsFromSynonymPath).ifPresent(value -> {
			// set value on model, return boolean whether to update mappings
			if (func.apply(value)) {
				// update mappings
				mappingsFromSynonymPath.forEach(m -> updateMappingSuccess(m, rosettaPath));
			}
		});
	}

	public static List<Mapping> filterMappings(List<Mapping> mappings, Path synonymPath) {
		return mappings.stream()
				.filter(p -> synonymPath.fullStartMatches(p.getXmlPath()))
				.collect(Collectors.toList());
	}

	public static List<Mapping> filterMappings(List<Mapping> mappings, RosettaPath rosettaPath) {
		return mappings.stream()
				.filter(m -> m.getRosettaPath() != null && m.getRosettaValue() != null)
				.filter(p -> rosettaPath.equals(toRosettaPath(p.getRosettaPath())))
				.collect(Collectors.toList());
	}

	/**
	 * After filtering mappings (either by synonym or cdm path), there are sometimes multiple mapping objects
	 * but there should be only one non-null value.
	 */
	public static Optional<String> getNonNullMappedValue(List<Mapping> filteredMappings) {
		return filteredMappings.stream()
				.map(Mapping::getXmlValue)
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.findFirst();
	}

	public static void updateMappings(Path synonymPath, List<Mapping> mappings, RosettaPath rosettaPath) {
		filterMappings(mappings, synonymPath).forEach(m -> updateMappingSuccess(m, rosettaPath));
	}

	public static void updateMappingSuccess(Mapping mapping, RosettaPath rosettaPath) {
		mapping.setRosettaPath(toPath(rosettaPath));
		mapping.setError(null);
		mapping.setCondition(true);
	}

	public static void updateMappingFail(Mapping mapping, String error) {
		mapping.setRosettaPath(null);
		mapping.setRosettaValue(null);
		mapping.setError(error);
		mapping.setCondition(true);
	}
}
