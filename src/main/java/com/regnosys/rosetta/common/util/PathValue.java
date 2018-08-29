package com.regnosys.rosetta.common.util;

public class PathValue {

    public static final PathValue EMPTY = new PathValue(HierarchicalPath.valueOf("generatedField"), "");

    private final HierarchicalPath hierarchicalPath;
    private final String value;

    public PathValue(HierarchicalPath hierarchicalPath, String value) {
        this.hierarchicalPath = hierarchicalPath;
        this.value = value;
    }

    public HierarchicalPath getHierarchicalPath() {
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
