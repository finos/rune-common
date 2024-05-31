package com.regnosys.rosetta.common.compile;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class JavaCSourceCancellableCompiler implements JavaCancellableCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaCSourceCancellableCompiler.class);
    public static final int DEFAULT_THREAD_POLL_INTERVAL_MS = 100;
    public static final int DEFAULT_MAX_COMPILE_TIMEOUT_MS = 300_000;

    private final int threadPollIntervalMs;
    private final int maxCompileTimeoutMs;
    private final ExecutorService executorService;
    private final boolean useSystemClassPath;
    private final boolean deleteOnError;
    private final boolean isVerbose;
    private final JavaCompileReleaseFlag releaseFlag;
    private final Path[] additionalClassPaths;

    public JavaCSourceCancellableCompiler(int threadPollIntervalMs,
                                          int maxCompileTimeoutMs,
                                          ExecutorService executorService,
                                          boolean useSystemClassPath,
                                          boolean deleteOnError,
                                          boolean isVerbose,
                                          JavaCompileReleaseFlag releaseFlag,
                                          Path... additionalClassPaths) {
        this.threadPollIntervalMs = threadPollIntervalMs;
        this.maxCompileTimeoutMs = maxCompileTimeoutMs;
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
        this(DEFAULT_THREAD_POLL_INTERVAL_MS,
                DEFAULT_MAX_COMPILE_TIMEOUT_MS,
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
                                         CancelIndicator cancelIndicator) throws ExecutionException, InterruptedException, TimeoutException {
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask compilationTask = getCompilationTask(sourceJavaPaths, targetPath, diagnosticCollector);

        Future<Boolean> submittedTask = executorService.submit(compilationTask);

        CompilationCompletionState compilationCompletionState = submitAndWait(submittedTask, cancelIndicator, targetPath);

        if (deleteOnError && compilationCompletionState == CompilationCompletionState.COMPILATION_FAILURES) {
            wipeTargetPath(targetPath);
        }

        return new JavaCompilationResult(compilationCompletionState,
                diagnosticCollector.getDiagnostics());
    }

    private void wipeTargetPath(Path targetPath) {
        try {
            FileUtils.cleanDirectory(targetPath.toFile());
        } catch (IOException e) {
            throw new CompilationTargetDeletionException("Failed to delete target classes after compilation error", e);
        }
    }

    private CompilationCompletionState submitAndWait(Future<Boolean> submittedTask, CancelIndicator cancelIndicator, Path targetPath) throws InterruptedException, ExecutionException, TimeoutException {
        int maxWaitCycles = maxCompileTimeoutMs / threadPollIntervalMs;

        for (int i = 0; i < maxWaitCycles; i++) {
            try {
                return submittedTask.get(threadPollIntervalMs, TimeUnit.MILLISECONDS) ?
                        CompilationCompletionState.COMPILATION_SUCCESS : CompilationCompletionState.COMPILATION_FAILURES;
            } catch (TimeoutException e) {
                if (cancelIndicator.isCancelled()) {
                    boolean cancellationAttemptedSuccessfully = submittedTask.cancel(true);
                    if (!cancellationAttemptedSuccessfully && submittedTask.isCancelled()) {
                        LOGGER.info("Attempted to cancel a compilation task but the cancellation attempt was unsuccessful, this may be because the task was already cancelled");
                    }
                    if (!submittedTask.isCancelled()) {
                        String message = String.format(
                        "Failed to cancel compile task while writing to %s. This may mean that the compilation process runs long or hangs.",
                                targetPath);
                        LOGGER.error(message);
                        throw new CompilationCancellationException(message);
                    }
                    return CompilationCompletionState.NOT_COMPLETE;
                }
                LOGGER.trace("Timed out whilst getting from task, iteration {}", i);
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
        classpath.append(File.pathSeparator);

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
