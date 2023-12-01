package com.regnosys.rosetta.common.compile;

import com.google.common.base.StandardSystemProperty;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JavaCSourceCancellableCompiler implements JavaCancellableCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaCSourceCancellableCompiler.class);

    private final int THREAD_POLL_INTERVAL_MS;
    private final int MAX_COMPILE_TIMEOUT_SECONDS;
    private final ExecutorService executorService;
    private final boolean useSystemClassPath;
    private final boolean deleteOnError;
    private final boolean isVerbose;
    private final JavaCompileReleaseFlag releaseFlag;
    private final Path[] additionalClassPaths;

    public JavaCSourceCancellableCompiler(int threadPollIntervalMs,
                                          int maxCompileTimeoutSeconds,
                                          ExecutorService executorService,
                                          boolean useSystemClassPath,
                                          boolean deleteOnError,
                                          boolean isVerbose,
                                          JavaCompileReleaseFlag releaseFlag,
                                          Path... additionalClassPaths) {
        this.THREAD_POLL_INTERVAL_MS = threadPollIntervalMs;
        this.MAX_COMPILE_TIMEOUT_SECONDS = maxCompileTimeoutSeconds;
        this.executorService = executorService;
        this.useSystemClassPath = useSystemClassPath;
        this.isVerbose = isVerbose;
        this.deleteOnError = deleteOnError;
        this.releaseFlag = releaseFlag;
        this.additionalClassPaths = additionalClassPaths;
    }

    public JavaCSourceCancellableCompiler(ExecutorService executorService,
                                          boolean useSystemClassPath,
                                          boolean deleteOnError,
                                          boolean isVerbose,
                                          JavaCompileReleaseFlag releaseFlag,
                                          Path... additionalClassPaths) {
        this(100,
                300,
                executorService,
                useSystemClassPath,
                deleteOnError,
                isVerbose,
                releaseFlag,
                additionalClassPaths);
    }

    @Override
    public JavaCompilationResult compile(List<Path> sourceJavaPaths,
                                         Path targetPath,
                                         Supplier<Boolean> isCancelled) throws ExecutionException, InterruptedException, TimeoutException {
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask compilationTask = getCompilationTask(sourceJavaPaths, targetPath, diagnosticCollector);

        Future<Boolean> submittedTask = executorService.submit(compilationTask);

        Optional<Boolean> result = submitAndWait(submittedTask, isCancelled);

        if (deleteOnError && result.isPresent() && !result.get()) {
            wipeTargetPath(targetPath);
        }

        return new JavaCompilationResult(result.isPresent(), result.orElse(false), diagnosticCollector.getDiagnostics());
    }

    private void wipeTargetPath(Path targetPath) {
        try {
            FileUtils.cleanDirectory(targetPath.toFile());
        } catch (IOException e) {
            throw new CompilationTargetDeletionException("Failed to delete target classes after compilation error", e);
        }
    }

    private Optional<Boolean> submitAndWait(Future<Boolean> submittedTask, Supplier<Boolean> isCancelled) throws InterruptedException, ExecutionException, TimeoutException {
        int maxWaitCycles = MAX_COMPILE_TIMEOUT_SECONDS * 1000 / THREAD_POLL_INTERVAL_MS;

        Optional<Boolean> result;
        for (int i = 0; i < maxWaitCycles; i++) {
            try {
                LOGGER.debug("Trying get from task");
                result = Optional.of(submittedTask.get(THREAD_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS));
                return result;
            } catch (TimeoutException e) {
                if (isCancelled.get()) {
                    boolean cancelled = submittedTask.cancel(true);
                    if (!cancelled && submittedTask.isCancelled()) {
                        LOGGER.warn("Attempted to cancel a compilation task but the cancellation attempt was unsuccessful, this may be because the task was already cancelled");
                    }
                    if (!submittedTask.isCancelled()) {
                        String message = "Attempt to cancel compilation task failed";
                        LOGGER.error(message);
                        throw new CompilationCancellationException(message);
                    }
                    return Optional.empty();
                }
                LOGGER.debug("Timed out whilst getting from task");
            }
        }
        throw new TimeoutException("Timed out waiting for compilation task");
    }


    private JavaCompiler.CompilationTask getCompilationTask(List<Path> sourceJavaPaths,
                                                            Path targetPath,
                                                            DiagnosticCollector<JavaFileObject> diagnosticCollector) {
        List<String> compilerArguments = createCompilerArguments(targetPath);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        List<File> sourceFiles = sourceJavaPaths.stream().map(Path::toFile).collect(Collectors.toList());
        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        return compiler.getTask(null, fileManager, diagnosticCollector, compilerArguments, null, javaFileObjects);
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

        args.add("-classpath");
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
