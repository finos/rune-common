package com.regnosys.rosetta.common.hashing;

import java.util.Arrays;

import com.rosetta.model.lib.RosettaKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

/**
 * Combines a {@link NonNullHashCollector} and {@link HashFunction} so that we have an easy way of asking a
 * {@link RosettaKey} model object for its hash value. The {@link NonNullHashCollector} ignores blank values such that
 *  * the hashcode can be used as a close proxy to equivalence.
 */
public class RosettaKeyValueHashFunction extends NonNullHashCollector implements Processor {

	private final IntegerHashGenerator hashcodeGenerator;
	
	public RosettaKeyValueHashFunction() {
		this.hashcodeGenerator = new IntegerHashGenerator();
	}
	
	@Override
	public <R extends RosettaModelObject> void processRosetta(RosettaPath path, Class<? extends R> rosettaType,
			R instance, RosettaModelObject parent, AttributeMeta... metas) {
		if (instance != null && !Arrays.stream(metas).anyMatch(m->m==AttributeMeta.IS_META || m==AttributeMeta.IS_ROSETTA_KEY)) {
			report.accumulate();
		}
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObject parent,
			AttributeMeta... metas) {
		if (instance!=null && !Arrays.stream(metas).anyMatch(m->m==AttributeMeta.IS_META || m==AttributeMeta.IS_ROSETTA_KEY)) {
			int hash = hashcodeGenerator.generate(instance);
			report.accumulate(hash);
		}

	}

}
