package com.regnosys.rosetta.common.compile;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;

/**
 * Provides success state and diagnostics for the results of Java compilations
 */
 public class JavaCompilationResult {

  private final boolean compilationSuccessful;
  private final List<Diagnostic<JavaFileObject>> diagnostics;

  public JavaCompilationResult(boolean compilationSuccessful, List<Diagnostic<JavaFileObject>> diagnostics) {
   this.compilationSuccessful = compilationSuccessful;
   this.diagnostics = diagnostics;
  }

  /**
   * Get the compilation success state.
   *
   * @return true if and only all the files compiled without errors;
   * false otherwise
   */
  public boolean isCompilationSuccessful() {
   return compilationSuccessful;
  }

 /**
  * Returns a list of Java diagnostics resulting from the compilation containing for example errors, warnings and compilation messages.
  * @return a list of diagnostics for {@code JavaFileObject}
  */
 public List<Diagnostic<JavaFileObject>> getDiagnostics() {
  return diagnostics;
 }
}
