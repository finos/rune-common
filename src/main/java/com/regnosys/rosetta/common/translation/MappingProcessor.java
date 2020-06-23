package com.regnosys.rosetta.common.translation;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class MappingProcessor implements BuilderProcessor {

	private final RosettaPath modelPath;
	private final List<Path> synonymPaths;
	private final List<Mapping> mappings;
	private final Map<Object, Object> params;

	public MappingProcessor(RosettaPath modelPath, List<Path> synonymPaths, MappingContext mappingContext) {
		this.modelPath = modelPath;
		this.synonymPaths = synonymPaths;
		this.mappings = mappingContext.getMappings();
		this.params = mappingContext.getMappingParams();
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath currentPath,
			Class<? extends R> rosettaType,
			RosettaModelObjectBuilder builder,
			RosettaModelObjectBuilder parent,
			AttributeMeta... meta) {
		if (builder != null && currentPath.matchesIgnoringIndex(modelPath)) {
			synonymPaths.forEach(p -> map(p, builder, parent));
		}
		return true;
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath currentPath,
			Class<? extends R> rosettaType,
			List<? extends RosettaModelObjectBuilder> builder,
			RosettaModelObjectBuilder parent,
			AttributeMeta... meta) {
		if (builder != null && matchesProcessorPathForMultipleCardinality(currentPath, rosettaType)) {
			synonymPaths.forEach(p -> map(p, builder, parent));
		}
		return true;
	}

	@Override
	public <T> void processBasic(RosettaPath currentPath, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
		if (currentPath.matchesIgnoringIndex(modelPath)) {
			synonymPaths.forEach(p -> mapBasic(p, Optional.ofNullable(instance), parent));
		}
	}

	@Override
	public <T> void processBasic(RosettaPath currentPath, Class<T> rosettaType, List<T> instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
		if (instance != null && currentPath.matchesIgnoringIndex(modelPath)) {
			synonymPaths.forEach(p -> mapBasic(p, instance, parent));
		}
	}

	@Override
	public Report report() {
		return null;
	}

	/**
	 * Perform custom mapping logic and updates resultant mapped value on builder object.
	 */
	protected void map(Path synonymPath, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	/**
	 * Perform custom mapping logic and updates resultant mapped value on builder object.
	 */
	protected void map(Path synonymPath, List<? extends RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	/**
	 * Perform custom mapping logic and updates resultant mapped value on builder object.
	 */
	protected <T> void mapBasic(Path synonymPath, Optional<T> instance, RosettaModelObjectBuilder parent) {
		instance.ifPresent(inst -> mapBasic(synonymPath, inst, parent));
	}

	protected <T> void mapBasic(Path synonymPath, T instance, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	/**
	 * Perform custom mapping logic and updates resultant mapped value on builder object.
	 */
	protected <T> void mapBasic(Path synonymPath, List<T> instance, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	protected RosettaPath getModelPath() {
		return modelPath;
	}

	protected List<Path> getSynonymPaths() {
		return synonymPaths;
	}

	protected List<Mapping> getMappings() {
		return mappings;
	}

	protected Map<Object, Object> getParams() {
		return params;
	}

	protected void setValueAndUpdateMappings(String synonymPath, Consumer<String> setter) {
		setValueAndUpdateMappings(Path.parse(synonymPath), setter);
	}

	protected void setValueAndUpdateMappings(Path synonymPath, Consumer<String> setter) {
		MappingProcessorUtils.setValueAndUpdateMappings(synonymPath, setter, mappings, modelPath);
	}

	private boolean matchesProcessorPathForMultipleCardinality(RosettaPath currentPath, Class<?> rosettaType) {
		return ReferenceWithMeta.class.isAssignableFrom(rosettaType) ?
				// so the parse handlers match on the list rather than each list item
				currentPath.matchesIgnoringIndex(modelPath.getParent()) :
				currentPath.matchesIgnoringIndex(modelPath);
	}
}