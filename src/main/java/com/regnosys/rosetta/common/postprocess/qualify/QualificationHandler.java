package com.regnosys.rosetta.common.postprocess.qualify;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;

/**
 * Qualification handler - implemented for each model.
 *
 * @param <Q> qualifiable class - the type to be qualified, e.g. EconomicTerms
 * @param <R> root class - the root type that contains both the qualified type and the qualifier, e.g. ContractualProduct
 * @param <B> root class builder - the root builder type, e.g. ContractualProductBuilder
 */
public interface QualificationHandler<Q extends RosettaModelObject, R extends RosettaModelObject, B extends RosettaModelObjectBuilder> {

    /**
     * The type to be qualified.
     */
    Class<Q> getQualifiableClass();

    /**
     * Gets qualifiable object instance from the root object.
     */
    Q getQualifiableObject(R rootObject);

    /**
     * Gets the qualifier from the root object.
     */
    String getQualifier(R rootObject);

    /**
     * Sets the qualifier on the root object builder.
     */
    void setQualifier(B rootObjectBuilder, String qualifier);
}
