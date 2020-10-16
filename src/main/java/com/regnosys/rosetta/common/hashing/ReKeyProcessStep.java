package com.regnosys.rosetta.common.hashing;

import com.regnosys.rosetta.common.hashing.GlobalKeyProcessStep.KeyPostProcessReport;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.GlobalKeyFields.GlobalKeyFieldsBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMetaBuilderBase;
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
	public <T extends RosettaModelObject> PostProcessorReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		ReKeyPostProcessReport report = new ReKeyPostProcessReport();
		ReKeyProcessor processor = new ReKeyProcessor(report, keyProcessor.runProcessStep(topClass, builder));
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
			Map<RosettaPath, GlobalKeyBuilder> globalKeyMap = keyPostProcessReport.getKeyMap();
			externalGlobalMap = new HashMap<>();
			for (Entry<RosettaPath, GlobalKeyBuilder> globalKey : globalKeyMap.entrySet()) {
				GlobalKeyFieldsBuilder meta = globalKey.getValue().getMeta();
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
			if (builder instanceof ReferenceWithMetaBuilderBase) {
				ReferenceWithMetaBuilderBase<?> reference = (ReferenceWithMetaBuilderBase<?>) builder;
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

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return null;
		}

	}

}
