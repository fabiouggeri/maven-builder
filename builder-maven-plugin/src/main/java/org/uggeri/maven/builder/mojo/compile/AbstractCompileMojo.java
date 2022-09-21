package org.uggeri.maven.builder.mojo.compile;

import org.uggeri.build.tools.compiler.project.ProjectCompilationListener;
import org.uggeri.build.tools.compiler.project.ProjectCompiler;
import org.uggeri.build.tools.compiler.project.CompilationException;
import org.uggeri.build.tools.compiler.project.ProjectCompilationResult;
import org.uggeri.build.tools.compiler.project.ProjectCompilationRequest;
import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.compiler.Compiler;
import org.uggeri.maven.builder.MavenLogWrapper;
import org.uggeri.build.tools.compiler.CompilationRequest;
import org.uggeri.build.tools.compiler.CompilationResult;
import org.uggeri.maven.builder.mojo.AbstractNativeMojo;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 * @author fabio_uggeri
 */
public abstract class AbstractCompileMojo extends AbstractNativeMojo implements Executor {

   private boolean verbose;

   private boolean showFileName;

   private File sourceDir;

   protected abstract List<File> getSourceFiles();

   protected abstract String getCompilationOutputDir();

   protected abstract String getSourcesDir();

   protected abstract void setOutputFiles(List<File> files);

   protected abstract boolean isSkipCompilation();

   @Override
   public void execute() throws MojoExecutionException {
      if (!isSkipCompilation()) {
         final int totalFiles = getSourceFiles().size();
         if (totalFiles > 0) {
            try {
               final ProjectCompilationRequest request = new ProjectCompilationRequest(getSourceFiles());
               final ProjectCompilationResult result;

               verbose = getVerbose().equalsIgnoreCase("true") || getVerbose().equalsIgnoreCase("full");
               showFileName = isShowFilename();
               sourceDir = new File(getSourcesDir());
               request.setCompilers(getBuildContext().getCompilers());
               request.setExecutor(getExecutor());
               request.setProperties(getExecutionProperties());
               request.setLog(new MavenLogWrapper(getLog()));
               request.setVerbose(verbose);
               request.setShowFileName(showFileName);
               request.setBatch(isBatch());
               request.setForce(isForce());
               request.setThreads(getParallelThreads());
               request.getIncludes().addAll(listIncludesPaths());
               request.getDefines().putAll(mapDefines());
               request.setSourceDir(sourceDir);
               request.setOutputDir(getCompilationOutputDir());
               request.setEnvironmentVariables(getBuildContext().getEnvironmentVariables());
               request.setPathSeparator(getPathSeparator());
               if (getParallelThreads() > 1) {
                  getLog().info("Compiling " + totalFiles + " source codes (" + getParallelThreads() + " threads)...");
               } else {
                  getLog().info("Compiling " + totalFiles + " source codes...");
               }
               result = new ProjectCompiler(request, new CompilationListenerImpl()).execute();
               verifyErrors(result);
               // Atualiza os arquivos para a proxima etapa...
               setOutputFiles(result.getOutputFiles());
            } catch (CompilationException ex) {
               throw new MojoExecutionException("Error in mojo " + getClass().getName(), ex);
            }
         }
      }
   }

   protected Set<String> loadIncludesExtensions() {
      final Set<String> headersExtensions = new HashSet<>();
      for (Compiler compiler : getBuildContext().getCompilers().values()) {
         headersExtensions.addAll(compiler.supportedIncludes());
      }
      return headersExtensions;
   }

   /**
    * @param defines the defines to set
    */
   public void setDefines(Map<String, String> defines) {
      this.defines = defines;
   }

   private void verifyErrors(ProjectCompilationResult compilationResult) throws MojoExecutionException {
      if (compilationResult.getErrors().size() > 0) {
         for (String error : compilationResult.getErrors()) {
            getLog().error(error);
         }
         throw new MojoExecutionException("Errors in compilation!");
      }
   }

   private synchronized void showCompilerOutput(CompilationResult result) {
      if (!result.getOutput().isEmpty()) {
         getLog().info("------------------------------------------------------------------------");
         for (String msg : result.getOutput()) {
            if (hasError(msg)) {
               getLog().error("| " + msg);
            } else if (hasWarning(msg)) {
               getLog().warn("| " + msg);
            } else {
               getLog().info("| " + msg);
            }
         }
         getLog().info("------------------------------------------------------------------------");
      }
   }

   private class CompilationListenerImpl implements ProjectCompilationListener {

      @Override
      public void compilationStarting(Compiler compiler, List<File> sources) {
         if (showFileName || verbose) {
            for (File source : sources) {
               getLog().info("Compiling " + BuildUtil.getRelativePath(sourceDir, source) + "...");
            }
         } else if (getLog().isDebugEnabled()) {
            if (!sources.isEmpty()) {
               getLog().debug("Compiling " + sources.toString());
            } else {
               getLog().debug("No files to compile.");
            }
         }
      }

      @Override
      public void compilationSucessful(Compiler compiler, CompilationRequest request, CompilationResult result) {
         if (getLog().isDebugEnabled()) {
            getLog().debug("Output objects: " + result.getOutputFiles());
            showCompilerOutput(result);
         } else if (verbose || hasWarnings(result)) {
            showCompilerOutput(result);
         }
      }

      @Override
      public void compilationFailed(Compiler compiler, CompilationRequest request, CompilationResult result) {
         showCompilerOutput(result);
      }

      @Override
      public void preCompilationValidationFailed(Compiler compiler, CompilationRequest request) {
      }

      @Override
      public void posCompilationValidationFailed(Compiler compiler, CompilationRequest request) {
      }

      @Override
      public void resultObjectsScheduled(Compiler compiler, CompilationRequest request, CompilationResult result) {
         if (getLog().isDebugEnabled()) {
            getLog().debug("Queued objects: " + result.getOutputFiles());
         }
      }

      @Override
      public void compilationFinished(Compiler compiler) {
      }
   }
}
