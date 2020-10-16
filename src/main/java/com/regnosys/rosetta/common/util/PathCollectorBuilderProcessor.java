package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PathCollectorBuilderProcessor implements BuilderProcessor {

	private final PathReport report = new PathReport();

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
			Class<R> rosettaType,
			RosettaModelObjectBuilder builder,
			RosettaModelObjectBuilder parent,
			AttributeMeta... meta) {
		return true;
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
			Class<R> rosettaType,
			List<? extends RosettaModelObjectBuilder> builders,
			RosettaModelObjectBuilder parent,
			AttributeMeta... meta) {
		return true;
	}

	@Override
	public <R> void processBasic(RosettaPath path, Class<R> rosettaType, R instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
		if (instance == null)
			return;
		report.collectedPaths.put(path, instance);
	}

	@Override
	public <R> void processBasic(RosettaPath path, Class<R> rosettaType, List<R> instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
		if (instance == null)
			return;
		int i = 0;
		for (Iterator<R> iterator = instance.iterator(); iterator.hasNext(); ) {
			R t = iterator.next();
			RosettaPath withIndex = path.withIndex(i++);
			report.collectedPaths.put(withIndex, t);
		}
	}

	@Override
	public PathReport report() {
		return report;
	}

	public static class PathReport implements BuilderProcessor.Report {
		private final Map<RosettaPath, Object> collectedPaths = new LinkedHashMap<>();

		public Map<RosettaPath, Object> getCollectedPaths() {
			return collectedPaths;
		}
	}
}
