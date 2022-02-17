package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.path.RosettaPath;

import java.util.Collections;
import java.util.List;

/**
 * Configurable options for ReferenceResolverProcessStep.
 *
 * @see com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep
 */
public class ReferenceResolverConfig {

    /**
     * @return empty config instance with no scope or excluded paths specified
     */
    public static ReferenceResolverConfig noScopeOrExcludedPaths() {
        return new ReferenceResolverConfig(null, Collections.emptyList());
    }

    private final Class<?> scopeType;
    private final List<RosettaPath> excludedPaths;

    public ReferenceResolverConfig(Class<?> scopeType, List<RosettaPath> excludedPaths) {
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
