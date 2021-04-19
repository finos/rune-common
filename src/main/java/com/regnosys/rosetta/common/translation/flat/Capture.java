package com.regnosys.rosetta.common.translation.flat;

import java.util.Map;

public class Capture {

	private final Map<String,Integer> indexes;
	private final String capturedValue;

	public Capture(Map<String,Integer>indexes , String capturedValue) {
		this.indexes = indexes;
		this.capturedValue = capturedValue;
	}

	public String getValue() {
		return capturedValue;
	}

	public Map<String,Integer> getIndexes() {
		return indexes;
	}
}
