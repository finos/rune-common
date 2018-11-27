package com.regnosys.rosetta.common.postprocess;

import com.rosetta.model.lib.RosettaModelObject;

public interface Visitor {

    void visit(RosettaModelObject object);

}
