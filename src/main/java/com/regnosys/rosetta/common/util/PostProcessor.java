package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.RosettaModelObject;

public interface PostProcessor {

    RosettaModelObject process(RosettaModelObject t);

    static PostProcessor empty() {
        return (RosettaModelObject t) -> t;
    }

}
