package com.regnosys.rosetta.common.compile;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public interface JavaCompiler {
    String JAVA_11 = "11";
    List<String> compile(List<Path> sourceJavaPaths,
                         Path outputClassesDir,
                         boolean useSystemClassPath,
                         boolean deleteOnError,
                         Supplier<Boolean> isCancelled,
                         String javaVersion,
                         Path... additionalClassPaths);
    default List<String> compile(List<Path> generateJavaPaths,
                                 Path outputClassesDir,
                                 boolean useSystemClassPath,
                                 boolean deleteOnError,
                                 Supplier<Boolean> isCancelled,
                                 Path... additionalClassPaths) {
        return compile(generateJavaPaths,
                outputClassesDir,
                useSystemClassPath,
                deleteOnError,
                isCancelled,
                JAVA_11,
                additionalClassPaths);
    }
}
