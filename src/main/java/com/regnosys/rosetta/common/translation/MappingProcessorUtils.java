package com.regnosys.rosetta.common.translation;

import com.regnosys.rosetta.common.util.PathUtils;
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

	public static Optional<String> getValueAndUpdateMappings(Path synonymPath, List<Mapping> mappings, RosettaPath rosettaPath) {
		List<Mapping> mappingsFromSynonymPath = filterMappings(mappings, synonymPath);
		Optional<String> mappedValue = getNonNullMappedValue(mappingsFromSynonymPath);
		mappedValue.ifPresent(value -> mappingsFromSynonymPath.forEach(m -> updateMappingSuccess(m, rosettaPath)));
		return mappedValue;
	}

	public static void setValueAndUpdateMappings(Path synonymPath, Consumer<String> setter, List<Mapping> mappings, RosettaPath rosettaPath) {
		getValueAndUpdateMappings(synonymPath, mappings, rosettaPath).ifPresent(setter::accept);
	}

	public static void setValueAndOptionallyUpdateMappings(Path synonymPath, Function<String, Boolean> func, List<Mapping> mappings, RosettaPath rosettaPath) {
		List<Mapping> mappingsFromSynonymPath = filterMappings(mappings, synonymPath);
		getNonNullMappedValue(mappingsFromSynonymPath).ifPresent(value -> {
			// set value on model, return boolean whether to update mappings
			boolean success = func.apply(value);
			// update mappings
			mappingsFromSynonymPath.forEach(m -> {
				if (success)
					updateMappingSuccess(m, rosettaPath);
				else
					updateMappingFail(m, "no destination");
			});
		});
	}

	public static List<Mapping> filterMappings(List<Mapping> mappings, Path synonymPath) {
		return mappings.stream()
				.filter(p -> synonymPath.nameIndexMatches(p.getXmlPath()))
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

	public static Optional<String> getNonNullMappedValue(Path synonymPath, List<Mapping> mappings) {
		return getNonNullMappedValue(filterMappings(mappings, synonymPath));
	}

	public static Optional<String> getNonNullMappedValue(List<Mapping> mappings, Path startsWith, String... endsWith) {
		return getNonNullMapping(mappings, startsWith, endsWith)
				.map(Mapping::getXmlValue)
				.map(String::valueOf);
	}

	public static Optional<Mapping> getNonNullMapping(List<Mapping> mappings, Path synonymPath) {
		return mappings.stream()
				.filter(m -> synonymPath.nameIndexMatches(m.getXmlPath()))
				.filter(m -> m.getXmlValue() != null)
				.findFirst();
	}

	public static Optional<Mapping> getNonNullMapping(List<Mapping> mappings, Path startsWith, String... endsWith) {
		return mappings.stream()
				.filter(m -> startsWith.fullStartMatches(m.getXmlPath()))
				.filter(m -> m.getXmlPath().endsWith(endsWith))
				.filter(m -> m.getXmlValue() != null)
				.findFirst();
	}

	public static Optional<Mapping> getNonNullMapping(List<Mapping> mappings, RosettaPath modelPathStartsWith, Path synonymPathStartsWith,
			String... synonymPathEndsWith) {
		return mappings.stream()
				.filter(m -> synonymPathStartsWith.fullStartMatches(m.getXmlPath()))
				.filter(m -> m.getXmlPath().endsWith(synonymPathEndsWith))
				.filter(m -> Optional.ofNullable(modelPathStartsWith).map(PathUtils::toPath).map(p -> p.fullStartMatches(m.getRosettaPath())).orElse(true))
				.filter(m -> m.getXmlValue() != null)
				.findFirst();
	}

	public static Optional<Path> subPath(String lastElement, Path path) {
		if (path.endsWith(lastElement)) {
			return Optional.of(path);
		}
		if (path.getElements().size() > 0) {
			return subPath(lastElement, path.getParent());
		}
		return Optional.empty();
	}

	public static void updateMappings(Path synonymPath, List<Mapping> mappings, RosettaPath rosettaPath) {
		mappings.stream()
				.filter(p -> synonymPath.fullStartMatches(p.getXmlPath()))
				.forEach(m -> updateMappingSuccess(m, rosettaPath));
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
