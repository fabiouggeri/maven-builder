/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.compiler.project;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.compiler.CompilationRequest;
import org.uggeri.build.tools.compiler.CompilationResult;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.util.FileUtils;
import org.uggeri.build.tools.compiler.Compiler;
import java.util.concurrent.Executor;

/**
 *
 * @author fabio_uggeri
 */
public class ProjectCompiler implements Executor {

   private final ProjectCompilationRequest request;

   private final ProjectCompilationResult result;

   private final ProjectCompilationListener compilationListener;

   private int workersCount = 0;

   private boolean executed = false;

   public ProjectCompiler(ProjectCompilationRequest execution, ProjectCompilationListener compilationListener) {
      this.request = execution;
      this.compilationListener = new CompilationListenerWrapper(compilationListener);
      this.result = new ProjectCompilationResult();
   }

   public ProjectCompilationResult execute() throws CompilationException {
      if (!executed) {
         try {
            /* Repete enquanto houver arquivos na fila para compilacao */
            do {
               scheduleSources(request);
               waitAllWorkers();
            } while (!request.getPendingSources().isEmpty());
         } finally {
            executed = true;
         }
      }
      return result;
   }

   private void scheduleSources(final ProjectCompilationRequest request) throws CompilationException {
      final Map<Compiler, List<File>> sourcesGroupedByCompiler = groupSourcesByCompiler(request);
      for (Map.Entry<Compiler, List<File>> compFiles : sourcesGroupedByCompiler.entrySet()) {
         final Compiler compiler = compFiles.getKey();
         /* Verifica se o compilador suporta compilacao em lote */
         if (request.isBatch() && compiler.isBatchSupport()) {
            Map<String, List<File>> sourcesByPath = groupSourcesByPath(request.getSourceDir(), compFiles.getValue());

            /* Agrupa os arquivos por path para compilacao, pois o diretorio de destino eh diferente para cada path */
            for (Map.Entry<String, List<File>> pathFiles : sourcesByPath.entrySet()) {
               final List<File> sources = pathFiles.getValue();
               int batchSize = sources.size() / request.getThreads();
               /* Se estiver configurado para executar compilacoes paralelas e
                o numero de fontes dividido pelo numero de threads for > 1,
                entao quebra os lotes em grupos menores */
               if (batchSize > 1) {
                  List<File> subset = new ArrayList<>();
                  for (File source : sources) {
                     subset.add(source);
                     /* Se o subconjunto eh >= ao tamaho do lote calculado, entao agenda execucao */
                     if (subset.size() >= batchSize) {
                        batchCompile(compiler, subset);
                        /* OBS.: cria um novo array, pois o atual sera passado para o
                         objeto BatchCompileWorker que nao faz uma copia dele */
                        subset = new ArrayList<>();
                     }
                  }
                  /* Se sobrou algum arquivo para compilar, entao agenda execucao */
                  if (subset.size() > 0) {
                     batchCompile(compiler, subset);
                  }
               } else {
                  batchCompile(compiler, sources);
               }
            }
         } else {
            for (File source : compFiles.getValue()) {
               singleCompile(compiler, source);
            }
         }
      }
   }

   private Map<Compiler, List<File>> groupSourcesByCompiler(ProjectCompilationRequest request) {
      Map<Compiler, List<File>> groupedFiles = new HashMap<>();
      for (File source : request.getPendingSources()) {
         Compiler compiler = request.getCompiler(source);
         if (compiler != null) {
            List<File> compilerFiles = groupedFiles.get(compiler);
            if (compilerFiles == null) {
               compilerFiles = new ArrayList<>();
               groupedFiles.put(compiler, compilerFiles);
            }
            compilerFiles.add(source);
         } else {
            result.getOutputFiles().add(source);
         }
      }
      request.getPendingSources().clear();
      return groupedFiles;
   }

   private Map<String, List<File>> groupSourcesByPath(File sourceDirectory, List<File> sources) {
      Map<String, List<File>> groupedFiles = new HashMap<>();
      for (File source : sources) {
         final String relativePath = FileUtils.getPath(BuildUtil.getRelativePath(sourceDirectory, source));
         List<File> compilerFiles = groupedFiles.get(relativePath);
         if (compilerFiles == null) {
            compilerFiles = new ArrayList<>();
            groupedFiles.put(relativePath, compilerFiles);
         }
         compilerFiles.add(source);
      }
      return groupedFiles;
   }

   private synchronized void waitAllWorkers() throws CompilationException {
      while (workersCount > 0) {
         try {
            wait();
         } catch (InterruptedException ex) {
            workersCount = 0;
            throw new CompilationException("Error waiting compilation.", ex);
         }
      }
   }

   private synchronized void incrementWorkersCount() {
      ++workersCount;
      notify();
   }

   private synchronized void decrementWorkersCount() {
      --workersCount;
      notify();
   }

   private void batchCompile(Compiler compiler, List<File> sources) throws CompilationException {
      try {
         incrementWorkersCount();
         getExecutor().execute(new BatchCompileWorker(request, result, compiler, sources, compilationListener));
      } catch (Exception ex) {
         throw new CompilationException("Compilation error!", ex);
      }
   }

   private void singleCompile(Compiler compiler, File source) throws CompilationException {
      try {
         incrementWorkersCount();
         getExecutor().execute(new SingleCompileWorker(request, result, compiler, source, compilationListener));
      } catch (Exception ex) {
         throw new CompilationException("Compilation error!", ex);
      }
   }

   private Executor getExecutor() {
      if (request.getExecutor() == null) {
         return this;
      }
      return request.getExecutor();
   }

   @Override
   public void execute(Runnable command) {
      command.run();
   }

   private class CompilationListenerWrapper implements ProjectCompilationListener {

      private final ProjectCompilationListener wrapped;

      public CompilationListenerWrapper(ProjectCompilationListener wrapped) {
         this.wrapped = wrapped;
      }

      @Override
      public void compilationStarting(Compiler compiler, List<File> sources) {
         wrapped.compilationStarting(compiler, sources);
      }

      @Override
      public void compilationSucessful(Compiler compiler, CompilationRequest request, CompilationResult result) {
         wrapped.compilationSucessful(compiler, request, result);
      }

      @Override
      public void compilationFailed(Compiler compiler, CompilationRequest request, CompilationResult result) {
         wrapped.compilationFailed(compiler, request, result);
      }

      @Override
      public void preCompilationValidationFailed(Compiler compiler, CompilationRequest request) {
         wrapped.preCompilationValidationFailed(compiler, request);
      }

      @Override
      public void posCompilationValidationFailed(Compiler compiler, CompilationRequest request) {
         wrapped.posCompilationValidationFailed(compiler, request);
      }

      @Override
      public void resultObjectsScheduled(Compiler compiler, CompilationRequest request, CompilationResult result) {
         wrapped.resultObjectsScheduled(compiler, request, result);
      }

      @Override
      public void compilationFinished(Compiler compiler) {
         decrementWorkersCount();
         wrapped.compilationFinished(compiler);
      }
   }

}
