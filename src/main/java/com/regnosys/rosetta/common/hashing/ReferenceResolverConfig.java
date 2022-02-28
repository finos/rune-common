package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.path.RosettaPath;

import java.util.Collections;
import java.util.List;

/**
 * For backwards compatibility.  Remove once CDM 2.148.0 is not longer used.
 */
@Deprecated
public class ReferenceResolverConfig extends ReferenceConfig {

    public ReferenceResolverConfig(Class<?> scopeType, List<RosettaPath> excludedPaths) {
       super(scopeType, excludedPaths);
    }
}
