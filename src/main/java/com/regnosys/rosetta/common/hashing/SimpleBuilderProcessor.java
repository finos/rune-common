package com.regnosys.rosetta.common.hashing;

import java.util.List;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

public abstract class SimpleBuilderProcessor implements BuilderProcessor{

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, List<? extends RosettaModelObjectBuilder> builders, RosettaModelObjectBuilder parent,
			AttributeMeta... metas) {
		if (builders==null) return false;
		boolean result=true;
		for (int i = 0; i < builders.size(); i++) {
			RosettaModelObjectBuilder builder = builders.get(i);
			path = path.withIndex(i);
			result &=processRosetta(path, rosettaType, builder, parent, metas);
		}
		return result;
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<T> rosettaType, List<T> instances, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
		if (instances==null) return;
		for (T instance:instances) {
			processBasic(path, rosettaType, instance, parent, metas);
		}
	}

}