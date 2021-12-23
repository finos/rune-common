package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;

public interface BackwardsCompatibleAnnotationIntrospector {
	JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated ann);
}
