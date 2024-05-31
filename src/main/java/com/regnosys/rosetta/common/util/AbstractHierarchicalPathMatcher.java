package com.regnosys.rosetta.common.util;

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

import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.path.RosettaPath.Element;

public abstract class AbstractHierarchicalPathMatcher implements HierarchicalPathMatcher {

    @Override
    public boolean matches(RosettaPath p1, RosettaPath p2) {
        if (p2 == null) return false;
        if (p1.getParent() != null ? !matches(p1.getParent(), p2.getParent()) : p2.getParent() != null) return false;
        return p1.getElement() != null ? matches(p1.getElement(), p2.getElement()) : p2.getElement() == null;
    }

    protected abstract boolean matches(Element e1, Element e2);

}
