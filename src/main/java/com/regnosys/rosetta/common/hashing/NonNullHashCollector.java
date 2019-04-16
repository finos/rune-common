package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.Processor;

import java.util.Arrays;

/**
 * A simple implementation of {@link Processor and BuilderProcessor} that only considers non-null
 * values.
 * For all non-null primitive values it uses the accumulate method of the integer report to accumulate a hashcode
 */
public class NonNullHashCollector extends SimpleBuilderProcessor implements Processor{

	private final IntegerHashGenerator hashcodeGenerator;
	protected final IntegerReport report;

	public NonNullHashCollector() {
		this.hashcodeGenerator = new IntegerHashGenerator();
		report = new IntegerReport(0);
	}

	@Override
	public <R extends RosettaModelObject> void processRosetta(RosettaPath path, Class<? extends R> rosettaType,
			R instance, RosettaModelObject parent, AttributeMeta... metas) {
		if (instance != null && !Arrays.stream(metas).anyMatch(m->m==AttributeMeta.IS_META)) {
			report.accumulate();
		}
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObject parent,
			AttributeMeta... metas) {
		if (instance!=null && !Arrays.stream(metas).anyMatch(m->m==AttributeMeta.IS_META)) {
			int hash = hashcodeGenerator.generate(instance);
			report.accumulate(hash);
		}

	}

	@Override
	public IntegerReport report() {
		return report;
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
			RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
		if (builder != null && !Arrays.stream(metas).anyMatch(m->m==AttributeMeta.IS_META)) {
			report.accumulate();
		}
		return true;
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance,
			RosettaModelObjectBuilder parent, AttributeMeta... metas) {
		if (instance!=null && !Arrays.stream(metas).anyMatch(m->m==AttributeMeta.IS_META)) {
			int hash = hashcodeGenerator.generate(instance);
			report.accumulate(hash);
		}
	}
}
