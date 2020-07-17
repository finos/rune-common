package com.regnosys.rosetta.common.translation;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base implementation does not do any mapping, but each mapping method can be overridden.
 */
public abstract class MappingProcessor implements MappingDelegate {

	private final RosettaPath modelPath;
	private final List<Path> synonymPaths;
	private final MappingContext context;

	public MappingProcessor(RosettaPath modelPath, List<Path> synonymPaths, MappingContext context) {
		this.modelPath = modelPath;
		this.synonymPaths = synonymPaths;
		this.context = context;
	}

	@Override
	public void map(Path synonymPath, Optional<RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent) {
		builder.ifPresent(b -> map(synonymPath, b, parent));
	}

	public void map(Path synonymPath, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	@Override
	public void map(Path synonymPath, List<? extends RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	@Override
	public <T> void mapBasic(Path synonymPath, Optional<T> instance, RosettaModelObjectBuilder parent) {
		instance.ifPresent(i -> mapBasic(synonymPath, i, parent));
	}

	public <T> void mapBasic(Path synonymPath, T instance, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	@Override
	public <T> void mapBasic(Path synonymPath, List<T> instance, RosettaModelObjectBuilder parent) {
		// Default behaviour - do nothing
	}

	@Override
	public RosettaPath getModelPath() {
		return modelPath;
	}

	@Override
	public List<Path> getSynonymPaths() {
		return synonymPaths;
	}

	protected List<Mapping> getMappings() {
		return context.getMappings();
	}

	protected MappingContext getContext() {
		return context;
	}

	protected void setValueAndUpdateMappings(String synonymPath, Consumer<String> setter) {
		setValueAndUpdateMappings(Path.parse(synonymPath), setter);
	}

	protected void setValueAndUpdateMappings(Path synonymPath, Consumer<String> setter) {
		MappingProcessorUtils.setValueAndUpdateMappings(synonymPath, setter, getMappings(), modelPath);
	}
}