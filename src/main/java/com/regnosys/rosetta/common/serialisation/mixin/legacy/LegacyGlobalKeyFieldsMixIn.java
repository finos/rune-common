package com.regnosys.rosetta.common.serialisation.mixin.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rosetta.model.lib.meta.Key;

import java.util.List;

public interface LegacyGlobalKeyFieldsMixIn {
    @JsonProperty("location")
    List<Key> getKey();
}
