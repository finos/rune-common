package com.regnosys.rosetta.common.hashing;

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
    private final Supplier<T> data;

    public ScopeReferenceHelper(ReferenceConfig referenceConfig, Supplier<T> data) {
        this.referenceConfig = referenceConfig;
        this.data = data;
    }

    public void collectScopePath(RosettaPath path, Class<?> rosettaType) {
        if (this.referenceConfig.getScopeType() != null && this.referenceConfig.getScopeType().isAssignableFrom(rosettaType)) {
            scopeToDataMap.putIfAbsent(PathUtils.toPath(path), data.get());
        }
    }

    public T getDataForModelPath(Path modelPath) {
        Path scopePath = getScopePath(modelPath);
        return scopeToDataMap.computeIfAbsent(scopePath, x -> data.get());
    }

    private Path getScopePath(Path modelPath) {
        Path scopePath = scopeToDataMap.keySet().stream()
                .filter(p -> p.fullStartMatches(modelPath))
                .findFirst()
                .orElse(EMPTY_SCOPE);
        return scopePath;
    }

    public Map<Path, T> getScopeToDataMap() {
        return scopeToDataMap;
    }
}