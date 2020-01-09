package com.regnosys.rosetta.common.util;

import com.regnosys.rosetta.common.hashing.SimpleBuilderProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor.Report;
import com.rosetta.model.lib.process.PostProcessStep;

import java.util.ArrayList;
import java.util.List;

public class RosettaObjectBuilderCollectorProcessStep<B extends RosettaModelObjectBuilder> implements PostProcessStep {

	private final Class<B> collectRosettaBuilderType;

	public RosettaObjectBuilderCollectorProcessStep(Class<B> collectRosettaBuilderType) {
		this.collectRosettaBuilderType = collectRosettaBuilderType;
	}

	@Override
	public Integer getPriority() {
		return 3;
	}

	@Override
	public String getName() {
		return "RosettaObjectBuilderCollector postProcessor";
	}

	@Override
	public <T extends RosettaModelObject> RosettaObjectBuilderCollectorProcessReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		List<B> collectedObjects = new ArrayList<>();
		RosettaObjectBuilderCollectorProcess<B> process = new RosettaObjectBuilderCollectorProcess<>(collectedObjects);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		process.processRosetta(path, topClass, builder, null);
		builder.process(path, process);
		return new RosettaObjectBuilderCollectorProcessReport(builder, collectedObjects);
	}
	
	private class RosettaObjectBuilderCollectorProcess<B extends RosettaModelObjectBuilder> extends SimpleBuilderProcessor {

		private final List<B> collectedObjects;

		public RosettaObjectBuilderCollectorProcess(List<B> collectedObjects) {
			this.collectedObjects = collectedObjects;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (builder == null || !builder.hasData()) {
				return false;
			}
			if (collectRosettaBuilderType.isInstance(builder)) {
				collectedObjects.add((B) builder);
			}
			return true;
		}
	
		@Override
		public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
		}
	
		@Override
		public Report report() {
			return null;
		}
	}
	
	public class RosettaObjectBuilderCollectorProcessReport<B extends RosettaModelObjectBuilder> implements PostProcessorReport, Report {

		private final B builder;
		private final List<B> collectedObjects;

		public RosettaObjectBuilderCollectorProcessReport(B builder, List<B> collectedObjects) {
			this.builder = builder;
			this.collectedObjects = collectedObjects;
		}

		@Override
		public B getResultObject() {
			return builder;
		}

		public List<B> getCollectedObjects() {
			return collectedObjects;
		}
	}
	
}
