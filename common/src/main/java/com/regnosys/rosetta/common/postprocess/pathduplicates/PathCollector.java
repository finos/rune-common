package com.regnosys.rosetta.common.postprocess.pathduplicates;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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

import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.Processor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Collects every basic-value path within a model object, so that duplicate paths can be detected.
 */
public class PathCollector<T extends RosettaModelObject> implements Processor, PostProcessStep {

	PathReport report;

	@Override
	public <W extends RosettaModelObject> PostProcessorReport runProcessStep(Class<? extends W> topClass,
																			 W builder) {
		report = new PathReport();
		builder.process(RosettaPath.valueOf(topClass.getSimpleName()), this);
		return report;
	}

	@Override
	public <R> void processBasic(RosettaPath path, Class<? extends R> rosettaType, R instance,
								 RosettaModelObject parent, AttributeMeta... meta) {
		if (instance == null) return;
		report.collectedPaths.put(path, instance);
	}

	@Override
	public <R> void processBasic(RosettaPath path, Class<? extends R> rosettaType, Collection<? extends R> instance,
								 RosettaModelObject parent, AttributeMeta... meta) {
		if (instance == null) return;
		int i = 0;
		for (Iterator<? extends R> iterator = instance.iterator(); iterator.hasNext();) {
			R t = iterator.next();
			RosettaPath withIndex = path.withIndex(i++);
			report.collectedPaths.put(withIndex, t);
		}
	}

	@Override
	public PathReport report() {
		return report;
	}

	public class PathReport implements Report, PostProcessorReport {
		private final Map<RosettaPath, Object> collectedPaths = new HashMap<>();
		private RosettaModelObjectBuilder result;

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return result;
		}

		public Map<RosettaPath, Object> getCollectedPaths() {
			return collectedPaths;
		}
	}

	@Override
	public Integer getPriority() {
		return 100;
	}

	@Override
	public String getName() {
		return "PathCollector";
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
																 R builder, RosettaModelObject parent, AttributeMeta... meta) {
		// path collector only cares about basic types
		return true;
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
																 List<? extends R> builder, RosettaModelObject parent, AttributeMeta... meta) {
		return true;
	}
}
