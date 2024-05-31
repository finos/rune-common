package com.regnosys.rosetta.common.util;

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

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor.Report;
import com.rosetta.model.lib.process.PostProcessStep;

import java.util.ArrayList;
import java.util.List;

public class RosettaObjectCollectorProcessStep<B extends RosettaModelObject> implements PostProcessStep {

	private final Class<B> collectRosettaType;

	public RosettaObjectCollectorProcessStep(Class<B> collectRosettaType) {
		this.collectRosettaType = collectRosettaType;
	}

	@Override
	public Integer getPriority() {
		return 3;
	}

	@Override
	public String getName() {
		return "RosettaObjectCollector postProcessor";
	}

	@Override
	public <T extends RosettaModelObject> RosettaObjectCollectorProcessReport<B> runProcessStep(Class<? extends T> topClass, T instance) {
		List<B> collectedObjects = new ArrayList<>();
		RosettaObjectCollectorProcess<B> process = new RosettaObjectCollectorProcess<>(collectRosettaType, collectedObjects);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		process.processRosetta(path, topClass, instance, null);
		instance.process(path, process);
		return new RosettaObjectCollectorProcessReport<>(instance, collectedObjects);
	}

	private static class RosettaObjectCollectorProcess<X extends RosettaModelObject> extends SimpleProcessor {

		private final Class<X> collectObjectType;
		private final List<X> collectedObjects;

		RosettaObjectCollectorProcess(Class<X> collectObjectType, List<X> collectedObjects) {
			this.collectObjectType = collectObjectType;
			this.collectedObjects = collectedObjects;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance,
				RosettaModelObject parent, AttributeMeta... metas) {
			if (instance == null) {
				return false;
			}
			if (collectObjectType.isInstance(instance)) {
				collectedObjects.add(collectObjectType.cast(instance));
			}
			return true;
		}

		@Override
		public Report report() {
			return null;
		}
	}

	public static class RosettaObjectCollectorProcessReport<C extends RosettaModelObject> implements PostProcessorReport, Report {

		private final RosettaModelObject topClass;
		private final List<C> collectedObjects;

		RosettaObjectCollectorProcessReport(RosettaModelObject topClass, List<C> collectedObjects) {
			this.topClass = topClass;
			this.collectedObjects = collectedObjects;
		}

		@Override
		public RosettaModelObject getResultObject() {
			return topClass;
		}

		public List<C> getCollectedObjects() {
			return collectedObjects;
		}
	}
}
