package com.regnosys.rosetta.common.model;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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

import java.util.Arrays;
import java.util.Objects;

public class MemoiseCacheKey {
    public static MemoiseCacheKey create(String name, Object... arguments) {
        return new MemoiseCacheKey(name, Arrays.asList(arguments));
    }
    private final String methodName;

    private final Object args;

    private MemoiseCacheKey(String methodName, Object args) {
        this.methodName = methodName;
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoiseCacheKey that = (MemoiseCacheKey) o;
        return Objects.equals(methodName, that.methodName) && Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, args);
    }
}
