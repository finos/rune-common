package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("ReferenceFilter")
public interface ReferenceWithMetaMixIn {
}
