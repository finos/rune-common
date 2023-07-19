package com.regnosys.rosetta.common.model;

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
