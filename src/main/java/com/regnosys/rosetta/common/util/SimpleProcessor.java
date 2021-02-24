package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;
import java.util.Collection;
import java.util.List;

public abstract class SimpleProcessor implements Processor {

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
			Class<? extends R> rosettaType,
			List<? extends R> instances,
			RosettaModelObject parent,
			AttributeMeta... metas) {
		if (instances == null)
			return false;
		boolean result = true;
		for (int i = 0; i < instances.size(); i++) {
			R instance = instances.get(i);
			path = path.withIndex(i);
			result &= processRosetta(path, rosettaType, instance, parent, metas);
		}
		return result;
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instances, RosettaModelObject parent, AttributeMeta... metas) {
		if (instances == null)
			return;
		for (T instance : instances) {
			processBasic(path, rosettaType, instance, parent, metas);
		}
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent, AttributeMeta... metas) {
		// Do nothing by default
	}
}