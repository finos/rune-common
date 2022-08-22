package com.regnosys.rosetta.common.postprocess.qualify;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;


public interface QualificationConfig<Q extends RosettaModelObject, R extends RosettaModelObject, B extends RosettaModelObjectBuilder> {

    Class<Q> getQualifiableClass();

    Q getQualifiableObject(R rootObject);

    String getQualifier(R rootObject);

    void setQualifier(B rootObjectBuilder, String qualifier);
}
