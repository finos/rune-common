package com.regnosys.rosetta.common.compile;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public interface JavaCancellableCompiler {
    JavaCompilationResult compile(List<Path> sourceJavaPaths,
                         Path targetPath,
                         Supplier<Boolean> isCancelled) throws ExecutionException, InterruptedException, TimeoutException;

}
