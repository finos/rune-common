package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.path.RosettaPath;

import java.util.Collections;
import java.util.List;

/**
 * Configurable options for ReferenceResolverProcessStep.
 *
 * @see com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep
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
