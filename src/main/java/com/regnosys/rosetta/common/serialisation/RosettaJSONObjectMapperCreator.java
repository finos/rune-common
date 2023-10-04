package com.regnosys.rosetta.common.serialisation;

import com.regnosys.rosetta.common.serialisation.mixin.RosettaJSONModule;

public class RosettaJSONObjectMapperCreator extends AbstractRosettaObjectMapperCreator {
    public RosettaJSONObjectMapperCreator(boolean supportNativeEnumValue) {
        super(supportNativeEnumValue, new RosettaJSONModule(supportNativeEnumValue));
    }
}
