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

import com.regnosys.rosetta.common.util.SimpleBuilderProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.lib.process.PostProcessStep;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author TomForwood
 * Calculates all the global key values for an object and it's children and returns them as a map from key->RosettaModelObject
 * It uses a Processor supplied in the constructor to do the actual calculation of hashes for applicable objects.
 */
public class GlobalKeyProcessStep implements PostProcessStep {

	private final Supplier<? extends Processor> hashCalculator;

	public GlobalKeyProcessStep(Supplier<? extends Processor> s) {
		this.hashCalculator = s;
	}

	@Override
	public Integer getPriority() {
		return 1;
	}

	@Override
	public String getName() {
		return "GlobalKey postProcessor";
	}

	@Override
	public <T extends RosettaModelObject> KeyPostProcessReport runProcessStep(Class<? extends T> topClass, T instance) {
		RosettaModelObjectBuilder builder = instance.toBuilder();
		KeyPostProcessReport thisReport = new KeyPostProcessReport(builder, new HashMap<>());
		ReKeyProcessor reKeyProcessor = new ReKeyProcessor(thisReport);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		reKeyProcessor.processRosetta(path, topClass, builder, null);
		builder.process(path, reKeyProcessor);
		return thisReport;
	}

	class ReKeyProcessor extends SimpleBuilderProcessor {
		KeyPostProcessReport report;

		ReKeyProcessor(KeyPostProcessReport report) {
			this.report = report;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
				Class<R> rosettaType,
				RosettaModelObjectBuilder builder,
				RosettaModelObjectBuilder parent,
				AttributeMeta... metas) {
			if (builder == null || !builder.hasData())
				return false;
			if (isGlobalKey(builder, metas)) {
				GlobalKey.GlobalKeyBuilder keyBuilder = (GlobalKey.GlobalKeyBuilder) builder;
				Processor hasher = hashCalculator.get();
				builder.process(path, hasher);
				Processor.Report rep = hasher.report();
				keyBuilder.getOrCreateMeta().setGlobalKey(rep.toString());
				report.keyMap.put(path, keyBuilder);
			}
			return true;
		}

		@Override
		public Report report() {
			return report;
		}

		private boolean isGlobalKey(RosettaModelObjectBuilder builder, AttributeMeta... metas) {
			return builder instanceof GlobalKey
					// exclude FieldWithMetas unless they contain a IS_GLOBAL_KEY_FIELD meta
					&& !(builder instanceof FieldWithMeta && !Arrays.asList(metas).contains(AttributeMeta.GLOBAL_KEY_FIELD));
		}
	}

	public class KeyPostProcessReport implements PostProcessorReport, BuilderProcessor.Report {

		private final RosettaModelObjectBuilder result;
		private final Map<RosettaPath, GlobalKey> keyMap;

		public KeyPostProcessReport(RosettaModelObjectBuilder result, Map<RosettaPath, GlobalKey> keyMap) {
			this.result = result;
			this.keyMap = keyMap;
		}

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return result;
		}

		public Map<RosettaPath, GlobalKey> getKeyMap() {
			return keyMap;
		}
	}
}
