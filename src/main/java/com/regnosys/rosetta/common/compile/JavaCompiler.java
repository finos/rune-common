package com.regnosys.rosetta.common.compile;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public interface JavaCompiler {
    JavaCompilationResult compile(List<Path> sourceJavaPaths,
                         Path targetClassesPath,
                         Supplier<Boolean> isCancelled);

}
