package com.regnosys.rosetta.common.util;

/*-
 * #%L
 * Rune Common
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

public class PathValue {

    public static final PathValue EMPTY = new PathValue(RosettaPath.valueOf("generatedField"), "");

    private final RosettaPath hierarchicalPath;
    private final String value;

    public PathValue(RosettaPath hierarchicalPath, String value) {
        this.hierarchicalPath = hierarchicalPath;
        this.value = value;
    }

    public RosettaPath getHierarchicalPath() {
        return hierarchicalPath;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PathValue that = (PathValue) o;

        if (hierarchicalPath != null ? !hierarchicalPath.equals(that.hierarchicalPath) : that.hierarchicalPath != null)
            return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = hierarchicalPath != null ? hierarchicalPath.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return hierarchicalPath.buildPath() + " -> " + value;
    }
}
