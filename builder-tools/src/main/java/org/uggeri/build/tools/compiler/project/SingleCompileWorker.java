/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.compiler.project;

import org.uggeri.build.tools.compiler.CompilationResult;
import org.uggeri.build.tools.compiler.Compiler;
import org.uggeri.build.tools.compiler.SingleCompilationRequest;
import java.io.File;
import java.util.Collections;

/**
 *
 * @author fabio_uggeri
 */
public class SingleCompileWorker implements Runnable {

   private final ProjectCompilationRequest projectRequest;
   private final ProjectCompilationResult projectResult;
   private final Compiler compiler;
   private final File source;
   private ProjectCompilationListener listener;

   public SingleCompileWorker(ProjectCompilationRequest request, ProjectCompilationResult result, Compiler compiler, File source, ProjectCompilationListener listener) {
      this.projectRequest = request;
      this.projectResult = result;
      this.compiler = compiler;
      this.source = source;
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
         SingleCompilationRequest compilationRequest = createRequest();
         CompilationResult compilationResult = null;
         if (listener != null) {
            listener.compilationStarting(compiler, Collections.singletonList(source));
         }
         if (compiler.preCompilation(source, compilationRequest)) {
            compilationResult = compiler.execute(compilationRequest);
            if (compilationResult.isSuccessful()) {
               if (listener != null) {
                  listener.compilationSucessful(compiler, compilationRequest, compilationResult);
               }
               if (!compiler.posCompilation(source, compilationResult.getOutputFiles().get(0), compilationRequest)) {
                  projectResult.addError(source.getPath() + " failed in pos-compilation.");
                  error = true;
                  if (listener != null) {
                     listener.posCompilationValidationFailed(compiler, compilationRequest);
                  }
               }
            } else {
               error = true;
               projectResult.addError(source.getPath() + " not compiled.");
               if (listener != null) {
                  listener.compilationFailed(compiler, compilationRequest, compilationResult);
               }
            }
         } else {
            error = true;
            projectResult.addError(source.getPath() + " failed in pre-compilation.");
            if (listener != null) {
               listener.preCompilationValidationFailed(compiler, compilationRequest);
            }
         }
         /*
          * Joga o arquivo objeto para a lista de arquivos a serem compilados...
          * Por exemplo: PRG -> C -> OBJ
          */
         if (!error && compilationResult != null) {
            synchronized (projectRequest.getPendingSources()) {
               projectRequest.getPendingSources().addAll(compilationResult.getOutputFiles());
            }
            if (listener != null) {
               listener.resultObjectsScheduled(compiler, compilationRequest, compilationResult);
            }
         }
      } finally {
         if (listener != null) {
            listener.compilationFinished(compiler);
         }
      }
   }

   private SingleCompilationRequest createRequest() {
      SingleCompilationRequest request = new SingleCompilationRequest(source);
      request.setDefines(projectRequest.getDefines());
      request.setIncludesPaths(projectRequest.getIncludes());
      request.setForce(projectRequest.isForce());
      request.setLog(projectRequest.getLog());
      request.setOutputDir(projectRequest.getOutputDir());
      request.setSourceDirectory(projectRequest.getSourceDir());
      request.setProperties(projectRequest.getProperties());
      request.setEnvironmentVariables(projectRequest.getEnvironmentVariables());
      return request;
   }

}
