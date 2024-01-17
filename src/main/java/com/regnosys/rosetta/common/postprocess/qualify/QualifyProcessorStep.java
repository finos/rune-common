package com.regnosys.rosetta.common.postprocess.qualify;

import com.google.inject.Inject;
import com.regnosys.rosetta.common.util.SimpleBuilderProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;

import java.util.*;
import java.util.function.Function;

public class QualifyProcessorStep implements PostProcessStep {
	
	@Inject
	QualifyFunctionFactory qualifyFunctionFactory;

	@Inject
	QualificationHandlerProvider qualificationHandlerProvider;

	@Override
	public Integer getPriority() {
		return 2;
	}

	@Override
	public String getName() {
		return "Qualification PostProcessor";
	}

	@Override
	public <T extends RosettaModelObject> QualificationReport runProcessStep(Class<? extends T> topClass, T instance) {
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		RosettaModelObjectBuilder builder = (RosettaModelObjectBuilder) instance;

		List<QualificationResult> collectedResults = new ArrayList<>();
		QualifyThenUpdateResultProcessor processor =
				new QualifyThenUpdateResultProcessor(qualificationHandlerProvider.getQualificationHandlerMap(), collectedResults);
		// check the top level object
		processor.processRosetta(path, topClass, builder, null);
		// check the rest
		builder.process(path, processor);

		return new QualificationReport(builder.build(), collectedResults);
	}

	private class QualifyThenUpdateResultProcessor extends SimpleBuilderProcessor {

		private final Map<Class<?>, QualificationHandler<?, ?, ?>> handlerMap;
		private final Set<Class<?>> rootTypes;
		private final List<QualificationResult> collectedResults;

		QualifyThenUpdateResultProcessor(Map<Class<?>, QualificationHandler<?, ?, ?>> handlerMap, List<QualificationResult> collectedResults) {
			this.handlerMap = handlerMap;
			this.rootTypes = handlerMap.keySet();
			this.collectedResults = collectedResults;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
																	 Class<R> rosettaType,
																	 RosettaModelObjectBuilder builder,
																	 RosettaModelObjectBuilder parent,
																	 AttributeMeta... metas) {
			if (builder == null)
				return false;

			if (rootTypes.contains(builder.getType())) {
				QualificationHandler<RosettaModelObject, R, RosettaModelObjectBuilder> handler =
						(QualificationHandler<RosettaModelObject, R, RosettaModelObjectBuilder>) handlerMap.get(builder.getType());
				RosettaModelObject qualifiableObject = handler.getQualifiableObject((R)builder.build());
				if (null != qualifiableObject) {
					QualificationResult result = qualify(handler.getQualifiableClass(), qualifiableObject);
					collectedResults.add(result);
					result.getUniqueSuccessQualifyResult().ifPresent(r->handler.setQualifier(builder, r.getName()));				}
				}
			return true;
		}

		@Override
		public Report report() {
			return null;
		}

		@SuppressWarnings("unchecked")
		private <R extends RosettaModelObject> QualificationResult qualify(Class<R> type, R instance) {
			RosettaMetaData<R> metaData = (RosettaMetaData<R>) instance.metaData();
			List<Function<? super R, QualifyResult>> qualifyFunctions = metaData.getQualifyFunctions(qualifyFunctionFactory);
			if (qualifyFunctions.isEmpty())
				return null;

			List<QualifyResult> allQualifyResults = new ArrayList<>();
			for (Function<? super R, QualifyResult> func:qualifyFunctions) {
				QualifyResult qualificationResult = func.apply(instance);
				allQualifyResults.add(qualificationResult);
			}
			return new QualificationResult(type, allQualifyResults);
		}
	}
}
