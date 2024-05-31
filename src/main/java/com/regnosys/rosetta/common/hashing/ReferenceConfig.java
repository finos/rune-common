package com.regnosys.rosetta.common.hashing;

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

import com.rosetta.model.lib.path.RosettaPath;

import java.util.Collections;
import java.util.List;

/**
 * Configurable options for ReferenceResolverProcessStep, and post-ingestion key/reference processing.
 *
 * @see com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep
 * @see com.regnosys.rosetta.common.hashing.ScopeReferenceHelper
 */
public class ReferenceConfig {

    /**
     * @return empty config instance with no scope or excluded paths specified
     */
    public static ReferenceConfig noScopeOrExcludedPaths() {
        return new ReferenceConfig(null, Collections.emptyList());
    }

    private final Class<?> scopeType;
    private final List<RosettaPath> excludedPaths;

    public ReferenceConfig(Class<?> scopeType, List<RosettaPath> excludedPaths) {
        this.scopeType = scopeType;
        this.excludedPaths = excludedPaths;
    }

    public Class<?> getScopeType() {
        return scopeType;
    }

    public List<RosettaPath> getExcludedPaths() {
        return excludedPaths;
    }
}
