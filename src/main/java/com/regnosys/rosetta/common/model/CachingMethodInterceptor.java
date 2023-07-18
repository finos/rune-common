package com.regnosys.rosetta.common.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class CachingMethodInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachingMethodInterceptor.class);

    private static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    private final Cache<MemoiseCacheKey, Object> memoiseCache;
    private final Set<String> debugFunctions;

    public CachingMethodInterceptor(CacheBuilder cacheBuilder, Set<String> debugFunctions) {
        this.memoiseCache = cacheBuilder.build();
        this.debugFunctions = debugFunctions;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MemoiseCacheKey key = MemoiseCacheKey.create(invocation.getMethod().toString(), invocation.getArguments());
        boolean debugLoggingEnabled = isDebugLoggingEnabled(invocation);

        Object ifPresent = memoiseCache.getIfPresent(key);
        if (ifPresent == null) {
            Object invoked = invocation.proceed();
            log(debugLoggingEnabled, "Executed function", invocation, invoked);

            if (invoked == null) {
                memoiseCache.put(key, NULL);
            } else {
                memoiseCache.put(key, invoked);
            }
            return invoked;
        }
        log(debugLoggingEnabled, "Cached function", invocation, ifPresent);

        if (ifPresent == NULL) {
            return null;
        }
        return ifPresent;
    }

    public Object invoke1(MethodInvocation invocation) throws Throwable {
        MemoiseCacheKey key = MemoiseCacheKey.create(invocation.getMethod().toString(), invocation.getArguments());
        boolean debugLoggingEnabled = isDebugLoggingEnabled(invocation);

        Object ifPresent = memoiseCache.getIfPresent(key);
        if (ifPresent == null) {
            Object functionResult = Optional.ofNullable(invocation.proceed()).orElse(NULL);
            log(debugLoggingEnabled, "Executed function", invocation, functionResult);
            memoiseCache.put(key, functionResult);
            return functionResult;
        }
        log(debugLoggingEnabled, "Cached function", invocation, ifPresent);
        if (ifPresent == NULL) {
            return null;
        }
        return ifPresent;
    }

    private boolean isDebugLoggingEnabled(MethodInvocation invocation) {
        return debugFunctions.contains(invocation.getMethod().getDeclaringClass().getSimpleName().toUpperCase());
    }

    private static void log(boolean debugLoggingEnabled, String message, MethodInvocation invocation, Object functionResult) {
        if (debugLoggingEnabled) {
            LOGGER.debug("{} '{}' Inputs[{}] Output[{}]",
                    message,
                    invocation.getMethod().getDeclaringClass().getSimpleName(),
                    invocation.getArguments(),
                    functionResult);
        }
    }
}
