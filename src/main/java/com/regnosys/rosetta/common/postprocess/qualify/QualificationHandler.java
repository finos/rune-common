package com.regnosys.rosetta.common.postprocess.qualify;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
