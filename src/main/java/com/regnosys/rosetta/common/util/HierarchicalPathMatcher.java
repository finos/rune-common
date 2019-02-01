package com.regnosys.rosetta.common.util;

import com.rosetta.model.lib.path.RosettaPath;

public interface HierarchicalPathMatcher {

    boolean matches(RosettaPath p1, RosettaPath p2);

}
