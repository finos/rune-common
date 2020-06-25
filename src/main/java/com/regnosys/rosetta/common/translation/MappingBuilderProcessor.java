package com.regnosys.rosetta.common.translation;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.List;
import java.util.Optional;

/**
 * Implements BuilderProcessor and delegates to the given MappingProcessor when the path matches.
 */
@SuppressWarnings("unused") // Used in xtend
public class MappingBuilderProcessor implements BuilderProcessor {

	private final MappingDelegate delegate;
	private final RosettaPath modelPath;
	private final List<Path> synonymPaths;

	public MappingBuilderProcessor(MappingDelegate delegate) {
		this.delegate = delegate;
		this.modelPath = delegate.getModelPath();
		this.synonymPaths = delegate.getSynonymPaths();
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath currentPath,
			Class<? extends R> rosettaType,
			RosettaModelObjectBuilder builder,
			RosettaModelObjectBuilder parent,
			AttributeMeta... meta) {
		if (builder != null && currentPath.equals(modelPath)) {
			synonymPaths.forEach(p -> delegate.map(p, builder, parent));
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
			synonymPaths.forEach(p -> delegate.map(p, builder, parent));
		}
		return true;
	}

	@Override
	public <T> void processBasic(RosettaPath currentPath, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
		if (currentPath.equals(modelPath)) {
			synonymPaths.forEach(p -> delegate.mapBasic(p, Optional.ofNullable(instance), parent));
		}
	}

	@Override
	public <T> void processBasic(RosettaPath currentPath, Class<T> rosettaType, List<T> instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
		if (instance != null && currentPath.equals(modelPath)) {
			synonymPaths.forEach(p -> delegate.mapBasic(p, instance, parent));
		}
	}

	@Override
	public BuilderProcessor.Report report() {
		return null;
	}

	private boolean matchesProcessorPathForMultipleCardinality(RosettaPath currentPath, Class<?> rosettaType) {
		return ReferenceWithMeta.class.isAssignableFrom(rosettaType) ?
				// so the parse handlers match on the list rather than each list item
				currentPath.equals(modelPath.getParent()) :
				currentPath.equals(modelPath);
	}
}
