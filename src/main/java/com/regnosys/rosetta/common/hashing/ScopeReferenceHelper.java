package com.regnosys.rosetta.common.hashing;

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

import com.regnosys.rosetta.common.translation.Path;
import com.regnosys.rosetta.common.util.PathUtils;
import com.rosetta.model.lib.path.RosettaPath;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ScopeReferenceHelper<T> {

    public static final Path EMPTY_SCOPE = Path.valueOf("emptyScope");

    private final ReferenceConfig referenceConfig;
    private final Map<Path, T> scopeToDataMap = new ConcurrentHashMap<>();
    private final Supplier<T> newDataStructureSupplier;

    public ScopeReferenceHelper(ReferenceConfig referenceConfig, Supplier<T> newDataStructureSupplier) {
        this.referenceConfig = referenceConfig;
        this.newDataStructureSupplier = newDataStructureSupplier;
    }

    public void collectScopePath(RosettaPath path, Class<?> rosettaType) {
        if (this.referenceConfig.getScopeType() != null && this.referenceConfig.getScopeType().isAssignableFrom(rosettaType)) {
            scopeToDataMap.putIfAbsent(PathUtils.toPath(path), newDataStructureSupplier.get());
        }
    }

    public T getDataForModelPath(Path modelPath) {
        Path scopePath = getScopePath(modelPath);
        return scopeToDataMap.computeIfAbsent(scopePath, x -> newDataStructureSupplier.get());
    }

    private Path getScopePath(Path modelPath) {
        return scopeToDataMap.keySet().stream()
                .filter(p -> p.fullStartMatches(modelPath))
                .findFirst()
                .orElse(EMPTY_SCOPE);
    }

    public Map<Path, T> getScopeToDataMap() {
        return scopeToDataMap;
    }
}
