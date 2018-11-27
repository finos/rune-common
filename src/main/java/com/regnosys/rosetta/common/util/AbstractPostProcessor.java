package com.regnosys.rosetta.common.util;

import com.regnosys.rosetta.common.postprocess.Visitor;
import com.rosetta.model.lib.RosettaModelObject;

import java.util.List;

public abstract class AbstractPostProcessor {

    protected List<Visitor> visitors;

    public void registerVisitors(List<Visitor> visitors) {
        this.visitors = visitors;
    }

    protected void visit(RosettaModelObject object) {
        visitors.forEach(visitor -> visitor.visit(object));
    }
}
