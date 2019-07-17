package com.regnosys.rosetta.common.hashing;

import java.util.Map;
import java.util.stream.Collectors;

import com.regnosys.rosetta.common.hashing.RosettaKeyProcessStep.KeyPostProcessReport;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMetaBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor.Report;
import com.rosetta.model.lib.process.PostProcessStep;

public class ReKeyProcessStep implements PostProcessStep{

	private final RosettaKeyProcessStep keyProcessor;
		
	public ReKeyProcessStep(RosettaKeyProcessStep keyProcessor) {
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
		private final Map<String, String> externalGlobalMap;
		
		public ReKeyProcessor(ReKeyPostProcessReport report, KeyPostProcessReport keyPostProcessReport) {
			super();
			this.report = report;
			Map<RosettaPath, GlobalKeyBuilder<?>> globalKeyMap = keyPostProcessReport.getKeyMap();
			externalGlobalMap = globalKeyMap.values().stream().map(GlobalKeyBuilder.class::cast)
				.map(k->k.getMeta())
				.filter(m->m.getExternalKey()!=null)
				.collect(Collectors.toMap(m->m.getExternalKey(), m->m.getGlobalKey(),
						(v1,v2)->{
							if (!v1.equals(v2))  throw new IllegalStateException(String.format("Duplicate key with differing values %s, %s", v1,v2));
							return v1;
						}));
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, RosettaModelObjectBuilder builder,
				RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (builder instanceof ReferenceWithMetaBuilder) {
				ReferenceWithMetaBuilder<?> reference = (ReferenceWithMetaBuilder<?>) builder;
				String externalReference = reference.getExternalReference();
				String globalRef = externalGlobalMap.get(externalReference);
				if (globalRef!=null) {
					reference.setGlobalReference(globalRef);
				}
				
			}
			return true;
		}

		@Override
		public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
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
