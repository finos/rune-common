package com.regnosys.rosetta.common.hashing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.PostProcessStep;

public class RosettaKeyProcessStep extends SimpleBuilderProcessor implements PostProcessStep {
	
	KeyPostProcessReport<?> report;
	private final Supplier<? extends BuilderProcessor> hashCalculator;
	
	public RosettaKeyProcessStep(Supplier<? extends BuilderProcessor> s) {
		this.hashCalculator = s;
	}

	@Override
	public Integer getPriority() {
		return 1;
	}

	@Override
	public String getName() {
		return "RosettaKey postProcessor";
	}

	@Override
	public <T extends RosettaModelObject> KeyPostProcessReport<T> runProcessStep(Class<T> topClass,
			RosettaModelObjectBuilder<T> builder) {
		KeyPostProcessReport<T> thisReport = new KeyPostProcessReport<>(builder, new HashMap<>());
		report = thisReport;
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		this.processRosetta(path, topClass, builder, null);
		builder.process(path, this);
		return thisReport;
	}

	@Override
	public <R extends RosettaModelObject> void processRosetta(RosettaPath path, Class<? extends R> rosettaType,
			RosettaModelObjectBuilder<R> builder, RosettaModelObjectBuilder<?> parent, AttributeMeta... metas) {
		if (builder instanceof RosettaKeyBuilder) {
			RosettaKeyBuilder<?> keyBuilder = (RosettaKeyBuilder<?>) builder;
			if (keyBuilder.getRosettaKey()==null) {
				BuilderProcessor hasher = hashCalculator.get();
				builder.process(path, hasher);
				Report rep = hasher.report();
				keyBuilder.setRosettaKey(rep.toString());
				report.keyMap.put(rep.toString(),builder);
			}
		}
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance,
			RosettaModelObjectBuilder<?> parent, AttributeMeta... metas) {
	}

	@Override
	public Report report() {
		return report;
	}
	
	public class KeyPostProcessReport<T extends RosettaModelObject> implements PostProcessorReport<T>, Report {

		private final RosettaModelObjectBuilder<T> result;
		private final Map<String, RosettaModelObjectBuilder<?>> keyMap;

		public KeyPostProcessReport(RosettaModelObjectBuilder<T> result, Map<String, RosettaModelObjectBuilder<?>> keyMap) {
			super();
			this.result = result;
			this.keyMap = keyMap;
		}

		@Override
		public RosettaModelObjectBuilder<T> getResultObject() {
			return result;
		}

		public Map<String, RosettaModelObjectBuilder<?>> getKeyMap() {
			return keyMap;
		}
	}
	
}
