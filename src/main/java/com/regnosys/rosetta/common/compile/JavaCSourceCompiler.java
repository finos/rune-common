package com.regnosys.rosetta.common.compile;

import com.google.common.base.StandardSystemProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JavaCSourceCompiler implements JavaCompiler {

    private final ExecutorService executorService;
    private final boolean useSystemClassPath;
    private final boolean deleteOnError;
    private final boolean isVerbose;
    private final JavaCompileReleaseFlag releaseFlag;
    private final Path[] additionalClassPaths;

    public JavaCSourceCompiler(ExecutorService executorService,
                               boolean useSystemClassPath,
                               boolean deleteOnError,
                               boolean isVerbose,
                               JavaCompileReleaseFlag releaseFlag,
                               Path... additionalClassPaths) {
        this.executorService = executorService;
        this.useSystemClassPath = useSystemClassPath;
        this.isVerbose = isVerbose;
        this.deleteOnError = deleteOnError;
        this.releaseFlag = releaseFlag;
        this.additionalClassPaths = additionalClassPaths;
    }

    @Override
    public JavaCompilationResult compile(List<Path> sourceJavaPaths,
                                         Path targetPath,
                                         Supplier<Boolean> isCancelled) {
        return null;
    }

    private List<String> createCompilerArguments(Path targetPath) {
        List<String> args = new ArrayList<>();

        if (isVerbose) {
            args.add("-verbose");
        } else {
            args.add("-nowarn");
            args.add("-g:none");
        }

        args.add("-d");
        args.add(targetPath.toAbsolutePath().toString());

        args.add(createClasspath(targetPath, useSystemClassPath, additionalClassPaths));
        return args;
    }

    private String createClasspath(Path targetPath, boolean useSystemClassPath, Path... additionalClassPaths) {
        StringBuilder classpath = new StringBuilder();

        classpath.append(targetPath.toAbsolutePath());
        classpath.append(StandardSystemProperty.PATH_SEPARATOR.value());

        if (useSystemClassPath) {
            classpath.append(StandardSystemProperty.JAVA_CLASS_PATH.value());
            classpath.append(StandardSystemProperty.PATH_SEPARATOR.value());
        }

        String additionalClassPathsString = Arrays.stream(additionalClassPaths)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(StandardSystemProperty.PATH_SEPARATOR.value()));

        classpath.append(additionalClassPathsString);



        return classpath.toString();
    }
}
