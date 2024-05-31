package com.regnosys.rosetta.common.translation;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

	public static List<String> getValueListAndUpdateMappings(Path synonymPath, List<Mapping> mappings, RosettaPath rosettaPath) {
		List<Mapping> mappingsFromSynonymPath = filterListMappings(mappings, synonymPath);
		List<String> mappedValues = getNonNullMappedValueList(mappingsFromSynonymPath);
		mappedValues.forEach(value -> mappingsFromSynonymPath.forEach(m -> updateMappingSuccess(m, rosettaPath)));
		return mappedValues;
	}

	public static void setValueAndUpdateMappings(Path synonymPath, Consumer<String> setter, List<Mapping> mappings, RosettaPath rosettaPath) {
		getValueAndUpdateMappings(synonymPath, mappings, rosettaPath).ifPresent(setter::accept);
	}

	public static void setValueListAndUpdateMappings(Path synonymPath, Consumer<String> setter, List<Mapping> mappings, RosettaPath rosettaPath) {
		getValueListAndUpdateMappings(synonymPath, mappings, rosettaPath).forEach(setter::accept);
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

	public static List<Mapping> filterListMappings(List<Mapping> mappings, Path synonymPath) {
		return mappings.stream()
				.filter(m -> pathListEquals(synonymPath, m.getXmlPath()))
				.collect(Collectors.toList());
	}

	public static List<Mapping> filterMappings(List<Mapping> mappings, Path synonymPath) {
		return mappings.stream()
				.filter(p -> synonymPath.nameIndexMatches(p.getXmlPath()))
				.collect(Collectors.toList());
	}

	/**
	 * Compare parent path on both name and index, but only compare the leaf on name.
	 */
	private static boolean pathListEquals(Path path1, Path path2) {
		if (path1.getElements().size() != path2.getElements().size()) {
			return false;
		}
		String pathName1 = path1.getLastElement().getPathName();
		String pathName2 = path2.getLastElement().getPathName();

		if (path1.getElements().size() > 1) {
			Path parentPath1 = path1.getParent();
			Path parentPath2 = path2.getParent();
			return parentPath1.nameIndexMatches(parentPath2) && pathName1.equals(pathName2);
		} else {
			return pathName1.equals(pathName2);
		}
	}

	public static List<Mapping> filterMappings(List<Mapping> mappings, RosettaPath rosettaPath) {
		return mappings.stream()
				.filter(m -> m.getRosettaPath() != null && m.getRosettaValue() != null)
				.filter(p -> rosettaPath.equals(toRosettaPath(p.getRosettaPath())))
				.collect(Collectors.toList());
	}

	public static List<Mapping> filterMappings(List<Mapping> mappings, Path synonymPath, Path startsWithModelPath) {
		return mappings.stream()
				.filter(m -> synonymPath.nameIndexMatches(m.getXmlPath()))
				.filter(m -> m.getRosettaPath() != null)
				.filter(m -> startsWithModelPath.fullStartMatches(m.getRosettaPath()))
				.collect(Collectors.toList());
	}

	public static List<Mapping> getEmptyMappings(List<Mapping> mappings, Path synonymPath) {
		return mappings.stream()
				.filter(p -> synonymPath.nameIndexMatches(p.getXmlPath()))
				.filter(m -> m.getRosettaPath() == null || m.getError() != null)
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

	public static List<String> getNonNullMappedValueList(List<Mapping> filteredMappings) {
		return filteredMappings.stream()
				.map(Mapping::getXmlValue)
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.collect(Collectors.toList());
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

	public static Optional<Mapping> getNonNullMappingForModelPath(List<Mapping> mappings, Path modelPath) {
		return mappings.stream()
				.filter(m -> m.getRosettaPath() != null)
				.filter(m -> modelPath.nameIndexMatches(m.getRosettaPath()))
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

	public static void updateMappingSuccess(Mapping mapping, Path rosettaPath) {
		mapping.setRosettaPath(rosettaPath);
		mapping.setError(null);
		mapping.setCondition(true);
		mapping.setDuplicate(false);
	}

	public static void updateMappingSuccess(Mapping mapping, RosettaPath rosettaPath) {
		updateMappingSuccess(mapping, toPath(rosettaPath));
	}

	public static void updateMappingFail(Mapping mapping, String error) {
		mapping.setRosettaPath(null);
		mapping.setRosettaValue(null);
		mapping.setError(error);
		mapping.setCondition(true);
	}
}
