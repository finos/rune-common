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

/**
 * @author TomForwood
 * Calculates all the RosettKey values for an object and it's children and returns them as a map from key->RosettaModelObject
 * It uses a BuilderProcessor supplied in the constructor to do the actual calculation of hashes for applicable objects.
 */
public class RosettaKeyProcessStep extends SimpleBuilderProcessor implements PostProcessStep {
	
	KeyPostProcessReport report;
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
	public <T extends RosettaModelObject> KeyPostProcessReport runProcessStep(Class<T> topClass,
			RosettaModelObjectBuilder builder) {
		KeyPostProcessReport thisReport = new KeyPostProcessReport(builder, new HashMap<>());
		report = thisReport;
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		this.processRosetta(path, topClass, builder, null);
		builder.process(path, this);
		return thisReport;
	}

	@Override
	public <R extends RosettaModelObject> void processRosetta(RosettaPath path, Class<? extends R> rosettaType,
			RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
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
			RosettaModelObjectBuilder parent, AttributeMeta... metas) {
	}

	@Override
	public Report report() {
		return report;
	}
	
	public class KeyPostProcessReport implements PostProcessorReport, Report {

		private final RosettaModelObjectBuilder result;
		private final Map<String, RosettaModelObjectBuilder> keyMap;

		public KeyPostProcessReport(RosettaModelObjectBuilder result, Map<String, RosettaModelObjectBuilder> keyMap) {
			super();
			this.result = result;
			this.keyMap = keyMap;
		}

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return result;
		}

		public Map<String, RosettaModelObjectBuilder> getKeyMap() {
			return keyMap;
		}
	}
	
}
