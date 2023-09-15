package com.regnosys.rosetta.common.serialisation.mixin.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface LegacyReferenceMixIn {
    @JsonProperty("value")
    String getReference();
}
