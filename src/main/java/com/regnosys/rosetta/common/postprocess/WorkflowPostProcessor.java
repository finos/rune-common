package com.regnosys.rosetta.common.postprocess;

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
