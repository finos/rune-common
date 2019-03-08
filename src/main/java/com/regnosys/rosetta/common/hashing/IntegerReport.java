package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;

/**
 * @author TomForwood
 * a Processor or BuilderProcessor report that contains an integer and allows accumulation too that integer to form a hash
 */
public class IntegerReport implements BuilderProcessor.Report, Processor.Report {
	int result;
	
	public IntegerReport(int result) {
		super();
		this.result = result;
	}

	public void accumulate() {
		result*=31;
	}
	
	public void accumulate(int newValue) {
		result = result*31+newValue;
	}

	public int getResult() {
		return result;
	}

	@Override
	public String toString() {
		return Integer.toHexString(result);
	}
}
