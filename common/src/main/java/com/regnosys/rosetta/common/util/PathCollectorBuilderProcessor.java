package com.regnosys.rosetta.common.util;

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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Deprecated // this implementation does not count meta paths correctly, use PathCountProcessor instead
public class PathCollectorBuilderProcessor implements Processor {

	private final PathReport report = new PathReport();

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
			Class<? extends R> rosettaType,
			R builder,
			RosettaModelObject parent,
			AttributeMeta... meta) {
		return true;
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
			Class<? extends R> rosettaType,
			List<? extends R> builders,
			RosettaModelObject parent,
			AttributeMeta... meta) {
		return true;
	}

	@Override
	public <R> void processBasic(RosettaPath path, Class<? extends R> rosettaType, R instance, RosettaModelObject parent, AttributeMeta... meta) {
		if (instance == null)
			return;
		report.collectedPaths.put(path, instance);
	}

	@Override
	public <R> void processBasic(RosettaPath path, Class<? extends R> rosettaType, Collection<? extends R> instance, RosettaModelObject parent, AttributeMeta... meta) {
		if (instance == null)
			return;
		int i = 0;
		for (Iterator<? extends R> iterator = instance.iterator(); iterator.hasNext(); ) {
			R t = iterator.next();
			RosettaPath withIndex = path.withIndex(i++);
			report.collectedPaths.put(withIndex, t);
		}
	}

	@Override
	public PathReport report() {
		return report;
	}

	public static class PathReport implements Processor.Report {
		private final Map<RosettaPath, Object> collectedPaths = new LinkedHashMap<>();

		public Map<RosettaPath, Object> getCollectedPaths() {
			return collectedPaths;
		}
	}
}
