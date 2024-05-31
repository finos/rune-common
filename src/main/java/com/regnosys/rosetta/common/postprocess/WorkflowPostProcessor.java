package com.regnosys.rosetta.common.postprocess;

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

import com.google.inject.Inject;
import com.regnosys.rosetta.common.hashing.*;
import com.regnosys.rosetta.common.postprocess.qualify.QualifyProcessorStep;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.PostProcessor;

import java.util.Arrays;
import java.util.List;

public class WorkflowPostProcessor implements PostProcessor {
	private final List<PostProcessStep> postProcessors;

	@Inject
	public WorkflowPostProcessor(QualifyProcessorStep qualifyProcessorStep, ReferenceConfig resolverConfig) {
		this.postProcessors = Arrays.asList(
				new ReKeyProcessStep(new GlobalKeyProcessStep(NonNullHashCollector::new)),
				new ReferenceResolverProcessStep(resolverConfig),
				qualifyProcessorStep);
	}

	@Override
	public <T extends RosettaModelObject> RosettaModelObjectBuilder postProcess(Class<T> rosettaType, RosettaModelObjectBuilder instance) {
		for (PostProcessStep postProcessor : postProcessors) {
			postProcessor.runProcessStep(rosettaType, instance);
		}
		return instance;
	}
}
