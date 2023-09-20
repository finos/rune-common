package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rosetta.model.lib.meta.Reference;

@JsonFilter("ReferenceFilter")
public interface ReferenceWithMetaMixIn {
    @JsonProperty("address")
    Reference getReference();
}
