package com.regnosys.rosetta.common.translation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingContext {

	private final List<Mapping> mappings;
	private final Map<Object, Object> mappingParams;

	public MappingContext() {
		this.mappings = new ArrayList<>();
		this.mappingParams = new HashMap<>();
	}

	public MappingContext(List<Mapping> mappings, Map<Object, Object> mappingParams) {
		this.mappings = mappings;
		this.mappingParams = mappingParams;
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public Map<Object, Object> getMappingParams() {
		return mappingParams;
	}
}
