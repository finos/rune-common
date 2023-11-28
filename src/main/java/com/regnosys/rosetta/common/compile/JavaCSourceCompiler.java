package com.regnosys.rosetta.common.compile;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class JavaCSourceCompiler implements JavaCompiler {

    private final ExecutorService executorService;
    private final boolean useSystemClassPath;
    private final boolean deleteOnError;
    private final String javaVersion;
    private final Path[] additionalClassPaths;

    public JavaCSourceCompiler(ExecutorService executorService,
                               boolean useSystemClassPath,
                               boolean deleteOnError,
                               String javaVersion,
                               Path... additionalClassPaths) {
        this.executorService = executorService;
        this.useSystemClassPath = useSystemClassPath;
        this.deleteOnError = deleteOnError;
        this.javaVersion = javaVersion;
        this.additionalClassPaths = additionalClassPaths;
    }

    @Override
    public List<String> compile(List<Path> sourceJavaPaths,
                                Path targetClassesPath,
                                Supplier<Boolean> isCancelled) {
        return null;
    }
}
