package com.regnosys.rosetta.common.hashing;

/*-
 * ==============
 * Rosetta Common
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

import com.regnosys.rosetta.common.hashing.GlobalKeyProcessStep.KeyPostProcessReport;
import com.regnosys.rosetta.common.util.SimpleBuilderProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor.Report;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.ProcessingException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ReKeyProcessStep implements PostProcessStep {

	private final GlobalKeyProcessStep keyProcessor;

	public ReKeyProcessStep(GlobalKeyProcessStep keyProcessor) {
		this.keyProcessor = keyProcessor;
	}

	@Override
	public <T extends RosettaModelObject> PostProcessorReport runProcessStep(Class<? extends T> topClass, T instance) {
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		RosettaModelObjectBuilder builder = instance.toBuilder();
		ReKeyPostProcessReport report = new ReKeyPostProcessReport(builder);
		ReKeyProcessor processor = new ReKeyProcessor(report, keyProcessor.runProcessStep(topClass, instance));
		processor.processRosetta(path, topClass, builder, null);
		builder.process(path, processor);
		return report;
	}

	@Override
	public Integer getPriority() {
		return 2;
	}

	@Override
	public String getName() {
		return "Re-key PostProcessor";
	}

	private class ReKeyProcessor extends SimpleBuilderProcessor {

		private final ReKeyPostProcessReport report;
		private Map<String, String> externalGlobalMap;

		public ReKeyProcessor(ReKeyPostProcessReport report, KeyPostProcessReport keyPostProcessReport) {
			super();
			this.report = report;
			Map<RosettaPath, GlobalKey> globalKeyMap = keyPostProcessReport.getKeyMap();
			externalGlobalMap = new HashMap<>();
			for (Entry<RosettaPath, GlobalKey> globalKey : globalKeyMap.entrySet()) {
				GlobalKeyFields meta = globalKey.getValue().getMeta();
				if (meta.getExternalKey() != null) {
					String external = meta.getExternalKey();
					String global = meta.getGlobalKey();
					if (externalGlobalMap.containsKey(external) && !externalGlobalMap.get(external).equals(global)) {
						throw new ProcessingException("Two distinct rosetta objects have the same external key " + external,
								globalKey.getValue().toString(), "ReKeyPostProcessor", globalKey.getKey());
					}
					externalGlobalMap.put(external, global);
				}
			}
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder,
				RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (builder instanceof ReferenceWithMeta.ReferenceWithMetaBuilder) {
				ReferenceWithMeta.ReferenceWithMetaBuilder<?> reference = (ReferenceWithMeta.ReferenceWithMetaBuilder<?>) builder;
				String externalReference = reference.getExternalReference();
				String globalRef = externalGlobalMap.get(externalReference);
				if (globalRef != null) {
					reference.setGlobalReference(globalRef);
				}
			}
			return true;
		}

		@Override
		public Report report() {
			return report;
		}

	}

	class ReKeyPostProcessReport implements Report, PostProcessorReport {

		private final RosettaModelObjectBuilder result;

		protected ReKeyPostProcessReport(RosettaModelObjectBuilder result) {
			super();
			this.result = result;
		}

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return result;
		}

	}

}
