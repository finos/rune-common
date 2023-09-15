package com.regnosys.rosetta.common.serialisation.mixin.legacy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rosetta.model.lib.RosettaModelObject;

public interface LegacyKeyMixIn {
    @JsonProperty("value")
    String getKeyValue();

    @JsonIgnore
    Class<? extends RosettaModelObject> getType();
}
