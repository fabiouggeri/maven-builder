package br.com.uggeri.maven.builder.mojo.compile;

import br.com.uggeri.build.tools.compiler.project.ProjectCompilationListener;
import br.com.uggeri.build.tools.compiler.project.ProjectCompiler;
import br.com.uggeri.build.tools.compiler.project.CompilationException;
import br.com.uggeri.build.tools.compiler.project.ProjectCompilationResult;
import br.com.uggeri.build.tools.compiler.project.ProjectCompilationRequest;
import br.com.uggeri.build.tools.ArtifactDependency;
import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.compiler.Compiler;
import br.com.uggeri.maven.builder.MavenLogWrapper;
import br.com.uggeri.build.tools.PackagingType;
import br.com.uggeri.build.tools.compiler.CompilationRequest;
import br.com.uggeri.build.tools.compiler.CompilationResult;
import br.com.uggeri.maven.builder.mojo.AbstractSicrediMojo;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @author fabio_uggeri
 */
public abstract class AbstractCompileMojo extends AbstractSicrediMojo implements Executor {

   /**
    * Defines condicionais. Declarados somente se uma condicao for satisfeita.
    */
   @Parameter
   protected List<ConditionalDefine> conditionalDefines;

   @Parameter
   protected Map<String, String> defines;

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
               request.setThreads(getBuildContext().getParallelThreads());
               setIncludes(request);
               setDefines(request);
               setConditionalDefines(request);
               request.setSourceDir(sourceDir);
               request.setOutputDir(getCompilationOutputDir());
               request.setEnvironmentVariables(getBuildContext().getEnvironmentVariables());
               if (getBuildContext().getParallelThreads() > 1) {
                  getLog().info("Compilando " + totalFiles + " arquivos fontes (" + getBuildContext().getParallelThreads() + " threads)...");
               } else {
                  getLog().info("Compilando " + totalFiles + " arquivos fontes...");
               }
               result = new ProjectCompiler(request, new CompilationListenerImpl()).execute();
               verifyErrors(result);
               // Atualiza os arquivos para a proxima etapa...
               setOutputFiles(result.getOutputFiles());
            } catch (CompilationException ex) {
               throw new MojoExecutionException("Erro mojo " + getClass().getName(), ex);
            }
         }
      }
   }

   /**
    * @return the conditional defines
    */
   public List<ConditionalDefine> getConditonalDefines() {
      return conditionalDefines;
   }

   /**
    * @return the defines
    */
   public Map<String, String> getDefines() {
      if (defines == null) {
         defines = new HashMap<String, String>();
      }
      return defines;
   }

   protected Set<String> loadIncludesExtensions() {
      final Set<String> headersExtensions = new HashSet<String>();
      for (Compiler compiler : getBuildContext().getCompilers().values()) {
         headersExtensions.addAll(compiler.supportedIncludes());
      }
      return headersExtensions;
   }

   private Set<ArtifactDependency> artifactsDependencies() {
      Set<ArtifactDependency> dependencies = new HashSet<ArtifactDependency>();
      for (Artifact artifact : (Set<Artifact>) getProject().getDependencyArtifacts()) {
         dependencies.add(new ArtifactDependency(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getClassifier(), artifact.getScope()));
      }
      return dependencies;
   }

   protected void setConditionalDefines(ProjectCompilationRequest execution) {
      if (getConditonalDefines() != null) {
         Set<ArtifactDependency> dependencies = artifactsDependencies();
         for (ConditionalDefine define : getConditonalDefines()) {
            if (define.isConditionSatisfied(dependencies, getProject().getProperties())) {
               execution.getDefines().put(define.getDefine(), define.getValue());
               getLog().debug("DEFINE: " + define.getDefine() + "=" + define.getValue());
            }
         }
      }
   }

   protected void setDefines(ProjectCompilationRequest execution) {
      for (Entry<String, String> define : getDefines().entrySet()) {
         execution.getDefines().put(define.getKey(), define.getValue());
         getLog().debug("DEFINE: " + define.getKey() + "=" + define.getValue());
      }
   }

   /**
    * @param defines the defines to set
    */
   public void setDefines(Map<String, String> defines) {
      this.defines = defines;
   }

   protected void setIncludes(ProjectCompilationRequest execution) throws MojoExecutionException {
      Set<String> headersExtensions = loadIncludesExtensions();
      for (Artifact artifact : getProject().getArtifacts()) {
         /*
          * Dependencia do tipo INCLUDE significa que eh um conjunto de arquivos header
          * zipados. Entao, tem que extrair para um diretorio temporario e esse diretorio
          * deve ser informado no momento da compilacao.
          */
         if (artifact.getType().equalsIgnoreCase(PackagingType.INCLUDE.toString())) {
            File dirExtract = new File(pathToExtractArtifact(artifact));
            if (!dirExtract.exists() || dirExtract.lastModified() < artifact.getFile().lastModified()) {
               createArtifactExtractionDirectory(artifact, dirExtract);
               getLog().info("Extraindo " + artifact.getId() + "...");
               if (!BuildUtil.extractZip(artifact.getFile(), dirExtract.getAbsolutePath(), false, new MavenLogWrapper(getLog()))) {
                  throw new MojoExecutionException("Erro ao extrair os includes do artefato " + artifact.getId() + ".");
               }
            }
            if (getLog().isDebugEnabled()) {
               for (String pathInc : BuildUtil.directoriesPathList(dirExtract)) {
                  getLog().debug("Adicionado include path: " + pathInc);
               }
            }
            execution.getIncludes().addAll(BuildUtil.directoriesPathList(dirExtract));
            /*
             * Se for dependencia de um artefato do tipo CH, H, API ou HPP, entao tem que
             * copiar o artefato para um diretorio temporario com seu nome original e informar
             * esse path no momento da compilacao.
             */
         } else if (artifact.getType().equalsIgnoreCase(PackagingType.FILE.name())) {
            if (headersExtensions.contains(BuildUtil.fileExtension(artifact.getArtifactId()))) {
               File dirExtract = createArtifactExtractionDirectory(artifact);
               File destFile = new File(dirExtract, artifact.getArtifactId());
               try {
                  if (!destFile.exists() || artifact.getFile().lastModified() > destFile.lastModified()) {
                     getLog().debug("Copiando o arquivo " + artifact.getFile().getAbsolutePath() + " para " + destFile.getAbsolutePath() + ".");
                     FileUtils.copyFile(artifact.getFile(), destFile);
                  }
                  if (getLog().isDebugEnabled()) {
                     getLog().debug("Adicionado include path: " + dirExtract.getAbsolutePath());
                  }
                  execution.getIncludes().add(dirExtract.getAbsolutePath());
               } catch (IOException ex) {
                  getLog().debug("Erro ao copiar o arquivo " + artifact.getFile().getAbsolutePath() + " para " + destFile.getAbsolutePath() + ".\nException: " + ex.getMessage());
                  throw new MojoExecutionException("Erro ao configurar dependencias.", ex);
               }
            }
         }
      }
      for (File headerFile : getBuildContext().getHeaderFiles()) {
         if (headerFile.isDirectory()) {
            if (getLog().isDebugEnabled()) {
               getLog().debug("Adicionado include path: " + headerFile.getAbsolutePath());
            }
            execution.getIncludes().add(headerFile.getAbsolutePath());
         } else {
            if (getLog().isDebugEnabled()) {
               getLog().debug("Adicionado include path: " + headerFile.getParentFile().getAbsolutePath());
            }
            execution.getIncludes().add(headerFile.getParentFile().getAbsolutePath());
         }
      }
   }

   private void verifyErrors(ProjectCompilationResult compilationResult) throws MojoExecutionException {
      if (compilationResult.getErrors().size() > 0) {
         for (String error : compilationResult.getErrors()) {
            getLog().error(error);
         }
         throw new MojoExecutionException("Erro durante a compilação!");
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
               getLog().info("Compilando " + BuildUtil.getRelativePath(sourceDir, source) + "...");
            }
         } else if (getLog().isDebugEnabled()) {
            if (!sources.isEmpty()) {
               getLog().debug("Compilando " + sources.toString());
            } else {
               getLog().debug("Nenhum arquivo para ser compilado.");
            }
         }
      }

      @Override
      public void compilationSucessful(Compiler compiler, CompilationRequest request, CompilationResult result) {
         if (getLog().isDebugEnabled()) {
            getLog().debug("Objetos Gerados: " + result.getOutputFiles());
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
            getLog().debug("Objetos enfileirados: " + result.getOutputFiles());
         }
      }

      @Override
      public void compilationFinished(Compiler compiler) {
      }
   }
}
