package com.regnosys.rosetta.common.compile;

import com.google.common.base.StandardSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JavaCSourceCompiler implements JavaCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaCSourceCompiler.class);
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

        args.add("-encoding");
        args.add("UTF-8");
        args.add("-proc:none");
        args.add("--release");
        args.add(releaseFlag.getVersion());

        return args;
    }

    private String createClasspath(Path targetPath, boolean useSystemClassPath, Path... additionalClassPaths) {
        StringBuilder classpath = new StringBuilder();

        classpath.append(targetPath.toAbsolutePath());
        classpath.append(StandardSystemProperty.PATH_SEPARATOR.value());

        if (useSystemClassPath) {
            String javaClassPath = StandardSystemProperty.JAVA_CLASS_PATH.value();
            if (javaClassPath != null && !javaClassPath.isEmpty()) {
                classpath.append(javaClassPath);
                classpath.append(File.pathSeparator);
            } else {
                LOGGER.warn("Compile called with useSystemClassPath flag set but the system classpath is empty, continuing compilation without");
            }
        }

        String additionalClassPathsString = Arrays.stream(additionalClassPaths)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));

        classpath.append(additionalClassPathsString);

        return classpath.toString();
    }
}
