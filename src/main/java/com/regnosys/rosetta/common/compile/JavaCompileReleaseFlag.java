package com.regnosys.rosetta.common.compile;

public enum JavaCompileReleaseFlag {
    JAVA_8("8"),
    JAVA_11("11"),
    JAVA_17("17");

    private final String version;

    JavaCompileReleaseFlag(String version) {

        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
