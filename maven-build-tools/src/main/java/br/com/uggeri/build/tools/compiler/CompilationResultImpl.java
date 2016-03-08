/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import br.com.uggeri.build.tools.AbstractExecutionResult;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public class CompilationResultImpl extends AbstractExecutionResult implements CompilationResult {

   private final List<File> outputFiles;

   public CompilationResultImpl(File outputFile) {
      if (outputFile != null) {
         outputFiles = Collections.singletonList(outputFile);
      } else {
         outputFiles = Collections.emptyList();
      }
   }

   public CompilationResultImpl(List<File> outputFiles) {
      this.outputFiles = outputFiles;
   }

   public CompilationResultImpl(List<File> outputFiles, Collection<String> output, int exitCode) {
      super(output, exitCode);
      this.outputFiles = outputFiles;
   }

   public CompilationResultImpl(File outputFile, Collection<String> output, int exitCode) {
      super(output, exitCode);
      if (outputFile != null) {
         outputFiles = Collections.singletonList(outputFile);
      } else {
         outputFiles = Collections.emptyList();
      }
   }
   
   @Override
   public List<File> getOutputFiles() {
      if (outputFiles == null) {
         return Collections.emptyList();
      }
      return outputFiles;
   }

   @Override
   public boolean isSuccessful() {
      return super.isSuccessful() && outputFiles != null && outputFiles.size() > 0;
   }

}
