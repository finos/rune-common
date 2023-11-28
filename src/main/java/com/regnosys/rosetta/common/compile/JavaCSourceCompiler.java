package com.regnosys.rosetta.common.compile;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class JavaCSourceCompiler implements JavaCompiler {

    private final ExecutorService executorService;

    public JavaCSourceCompiler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public List<String> compile(List<Path> sourceJavaPaths, Path outputClassesDir, boolean useSystemClassPath, boolean deleteOnError, Supplier<Boolean> isCancelled, String javaVersion, Path... additionalClassPaths) {
        return null;
    }
}
