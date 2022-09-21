/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.compiler.project;

import org.uggeri.build.tools.compiler.BatchCompilationRequest;
import org.uggeri.build.tools.compiler.CompilationResult;
import org.uggeri.build.tools.compiler.Compiler;
import java.io.File;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
class BatchCompileWorker implements Runnable {

   private final ProjectCompilationRequest projCompilationRequest;
   private final ProjectCompilationResult projCompilationResult;
   private final Compiler compiler;
   private final List<File> sources;
   private ProjectCompilationListener listener;

   public BatchCompileWorker(ProjectCompilationRequest request, ProjectCompilationResult result, Compiler compiler, List<File> sources, ProjectCompilationListener listener) {
      this.projCompilationRequest = request;
      this.projCompilationResult = result;
      this.compiler = compiler;
      this.sources = sources;
      this.listener = listener;
   }

   public ProjectCompilationListener getListener() {
      return listener;
   }

   public void setListener(ProjectCompilationListener listener) {
      this.listener = listener;
   }

   @Override
   public void run() {
      try {
         boolean error = false;
         if (listener != null) {
            listener.compilationStarting(compiler, sources);
         }
         /* Compila somente se restam arquivos fontes na lista */
         if (!sources.isEmpty()) {
            BatchCompilationRequest compilationRequest = createRequest();
            CompilationResult compilationResult = null;
            if (batchPreCompilation(compiler, compilationRequest)) {
               compilationResult = compiler.execute(compilationRequest);
               if (compilationResult.isSuccessful()) {
                  if (listener != null) {
                     listener.compilationSucessful(compiler, compilationRequest, compilationResult);
                  }
                  if (!batchPosCompilation(compiler, compilationRequest, compilationResult)) {
                     error = true;
                     if (listener != null) {
                        listener.posCompilationValidationFailed(compiler, compilationRequest);
                     }
                  }
               } else {
                  error = true;
                  addObjectsNotFoundErrors(compilationResult.getOutputFiles());
                  if (listener != null) {
                     listener.compilationFailed(compiler, compilationRequest, compilationResult);
                  }
               }
            } else {
               error = true;
               if (listener != null) {
                  listener.preCompilationValidationFailed(compiler, compilationRequest);
               }
            }
            /*
             * Se nao houve erro agenda a execucao dos arquivos compilados...
             * Por exemplo: PRG -> C -> OBJ
             */
            if (!error && compilationResult != null) {
               synchronized (projCompilationRequest.getPendingSources()) {
                  projCompilationRequest.getPendingSources().addAll(compilationResult.getOutputFiles());
               }
               if (listener != null) {
                  listener.resultObjectsScheduled(compiler, compilationRequest, compilationResult);
               }
            }
         }
      } finally {
         if (listener != null) {
            listener.compilationFinished(compiler);
         }
      }
   }

   private boolean batchPreCompilation(Compiler compiler, BatchCompilationRequest compilationRequest) {
      boolean valid = true;
      for (File source : sources) {
         if (!compiler.preCompilation(source, compilationRequest)) {
            projCompilationResult.addError(source.getPath() + " not compiled. Pre-compilation failed.");
            valid = false;
         }
      }
      return valid;
   }

   private void addObjectsNotFoundErrors(List<File> outputFiles) {
      for (File outFile : outputFiles) {
         if (!outFile.exists()) {
            projCompilationResult.addError(outFile.getPath() + " not compiled.");
         }
      }
   }

   private boolean batchPosCompilation(Compiler compiler, BatchCompilationRequest request, CompilationResult compilationResult) {
      boolean valid = true;
      for (int i = 0; i < request.getSources().size(); i++) {
         final File source = request.getSources().get(i);
         final File output = compilationResult.getOutputFiles().get(i);
         if (!compiler.posCompilation(source, output, request)) {
            projCompilationResult.addError(source.getPath() + " failed in pos-compilation.");
            valid = false;
         }
      }
      return valid;
   }

   private BatchCompilationRequest createRequest() {
      BatchCompilationRequest compileRequest = new BatchCompilationRequest(sources);
      compileRequest.setDefines(projCompilationRequest.getDefines());
      compileRequest.setIncludesPaths(projCompilationRequest.getIncludes());
      compileRequest.setForce(projCompilationRequest.isForce());
      compileRequest.setLog(projCompilationRequest.getLog());
      compileRequest.setOutputDir(projCompilationRequest.getOutputDir());
      compileRequest.setSourceDirectory(projCompilationRequest.getSourceDir());
      compileRequest.setProperties(projCompilationRequest.getProperties());
      compileRequest.setEnvironmentVariables(projCompilationRequest.getEnvironmentVariables());
      return compileRequest;
   }

}
