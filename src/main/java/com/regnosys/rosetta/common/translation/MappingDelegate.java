package com.regnosys.rosetta.common.translation;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MappingDelegate {

	void map(Path synonymPath, Optional<RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent);

	void map(Path synonymPath, List<? extends RosettaModelObjectBuilder> builder, RosettaModelObjectBuilder parent);

	<T> void mapBasic(Path synonymPath, Optional<T> instance, RosettaModelObjectBuilder parent);

	<T> void mapBasic(Path synonymPath, Collection<? extends T> instance, RosettaModelObjectBuilder parent);

	RosettaPath getModelPath();

	List<Path> getSynonymPaths();
}
