package com.regnosys.rosetta.common.hashing;

import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMetaBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.BuilderProcessor.Report;
import com.rosetta.model.lib.process.PostProcessStep;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author TomForwood
 * Calculates all the global key values for an object and it's children and returns them as a map from key->RosettaModelObject
 * It uses a BuilderProcessor supplied in the constructor to do the actual calculation of hashes for applicable objects.
 */
public class GlobalKeyProcessStep implements PostProcessStep {

	private final Supplier<? extends BuilderProcessor> hashCalculator;

	public GlobalKeyProcessStep(Supplier<? extends BuilderProcessor> s) {
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
	public <T extends RosettaModelObject> KeyPostProcessReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		KeyPostProcessReport thisReport = new KeyPostProcessReport(builder, new HashMap<>());
		KeyProcessProcess process = new KeyProcessProcess(thisReport);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		process.processRosetta(path, topClass, builder, null);
		builder.process(path, process);
		return thisReport;
	}

	class KeyProcessProcess extends SimpleBuilderProcessor {
		KeyPostProcessReport report;

		KeyProcessProcess(KeyPostProcessReport report) {
			this.report = report;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
				Class<? extends R> rosettaType,
				RosettaModelObjectBuilder builder,
				RosettaModelObjectBuilder parent,
				AttributeMeta... metas) {
			if (builder == null || !builder.hasData())
				return false;
			if (isGlobalKey(builder, metas)) {
				GlobalKeyBuilder keyBuilder = (GlobalKeyBuilder) builder;
				BuilderProcessor hasher = hashCalculator.get();
				builder.process(path, hasher);
				Report rep = hasher.report();
				keyBuilder.getOrCreateMeta().setGlobalKey(rep.toString());
				report.keyMap.put(path, keyBuilder);
			}
			return true;
		}

		@Override
		public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance,
				RosettaModelObjectBuilder parent, AttributeMeta... metas) {
		}

		@Override
		public Report report() {
			return report;
		}

		private boolean isGlobalKey(RosettaModelObjectBuilder builder, AttributeMeta... metas) {
			return builder instanceof GlobalKeyBuilder
					// exclude FieldWithMetas unless they contain a IS_GLOBAL_KEY_FIELD meta
					&& !(builder instanceof FieldWithMetaBuilder && !Arrays.asList(metas).contains(AttributeMeta.IS_GLOBAL_KEY_FIELD));
		}
	}

	public class KeyPostProcessReport implements PostProcessorReport, Report {

		private final RosettaModelObjectBuilder result;
		private final Map<RosettaPath, GlobalKeyBuilder> keyMap;

		public KeyPostProcessReport(RosettaModelObjectBuilder result, Map<RosettaPath, GlobalKeyBuilder> keyMap) {
			this.result = result;
			this.keyMap = keyMap;
		}

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return result;
		}

		public Map<RosettaPath, GlobalKeyBuilder> getKeyMap() {
			return keyMap;
		}
	}

}
