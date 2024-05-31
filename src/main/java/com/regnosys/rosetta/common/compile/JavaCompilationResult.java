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

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;

/**
 * Provides success state and diagnostics for the results of Java compilations
 */
public class JavaCompilationResult {

    private final CompilationCompletionState compilationCompletionState;
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    public JavaCompilationResult(CompilationCompletionState compilationCompletionState,
                                 List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        this.compilationCompletionState = compilationCompletionState;
        this.diagnostics = diagnostics;
    }

    /**
     * Get the compilation success state.
     *
     * @return true if and only if all the files compiled without errors;
     * false otherwise
     */
    public boolean isCompilationSuccessful() {
        return compilationCompletionState == CompilationCompletionState.COMPILATION_SUCCESS;
    }

    /**
     * Get the compilation completion state
     * @return {@code CompilationCompletionState} which enumerates whether the compilation completed successfully,
     * completed with failures or did not complete at all. A failure to complete entirely could be due to
     * a timeout, a cancellation or an execution error in the complication process.
     */
    public CompilationCompletionState getCompilationCompletionState() {
        return compilationCompletionState;
    }

    /**
     * Returns a list of Java diagnostics resulting from the compilation containing for example errors, warnings and compilation messages.
     *
     * @return a list of diagnostics for {@code JavaFileObject}
     */
    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics;
    }
}
