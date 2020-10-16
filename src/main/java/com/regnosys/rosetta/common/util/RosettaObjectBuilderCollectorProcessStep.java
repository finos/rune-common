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
	public <T extends RosettaModelObject> RosettaObjectBuilderCollectorProcessReport<B> runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		List<B> collectedObjects = new ArrayList<>();
		RosettaObjectBuilderCollectorProcess<B> process = new RosettaObjectBuilderCollectorProcess<>(collectRosettaBuilderType, collectedObjects);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		process.processRosetta(path, topClass, builder, null);
		builder.process(path, process);
		return new RosettaObjectBuilderCollectorProcessReport<>(builder, collectedObjects);
	}

	private static class RosettaObjectBuilderCollectorProcess<X extends RosettaModelObjectBuilder> extends SimpleBuilderProcessor {

		private final Class<X> collectObjectType;
		private final List<X> collectedObjects;

		RosettaObjectBuilderCollectorProcess(Class<X> collectObjectType, List<X> collectedObjects) {
			this.collectObjectType = collectObjectType;
			this.collectedObjects = collectedObjects;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (builder == null || !builder.hasData()) {
				return false;
			}
			if (collectObjectType.isInstance(builder)) {
				collectedObjects.add(collectObjectType.cast(builder));
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
	
	public static class RosettaObjectBuilderCollectorProcessReport<C extends RosettaModelObjectBuilder> implements PostProcessorReport, Report {

		private final RosettaModelObjectBuilder topClassBuilder;
		private final List<C> collectedBuilders;

		RosettaObjectBuilderCollectorProcessReport(RosettaModelObjectBuilder topClassBuilder, List<C> collectedBuilders) {
			this.topClassBuilder = topClassBuilder;
			this.collectedBuilders = collectedBuilders;
		}

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return topClassBuilder;
		}

		public List<C> getCollectedObjects() {
			return collectedBuilders;
		}
	}
}
