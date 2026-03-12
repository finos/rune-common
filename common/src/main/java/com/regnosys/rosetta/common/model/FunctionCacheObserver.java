package com.regnosys.rosetta.common.model;

import org.aopalliance.intercept.MethodInvocation;

public interface FunctionCacheObserver {
    /**
     * Called for each memoisation lookup before returning from the caching interceptor.
     */
    void onCacheLookup(MethodInvocation invocation, MemoiseCacheKey cacheKey, boolean cacheHit, Object cachedValue) throws Throwable;
}
