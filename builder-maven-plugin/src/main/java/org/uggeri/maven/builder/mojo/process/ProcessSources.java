/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo.process;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.PackagingType;
import org.uggeri.maven.builder.file.SourceFileScanException;
import org.uggeri.maven.builder.file.SourceFileScanner;
import org.uggeri.maven.builder.file.SourceFileScannerListener;
import org.uggeri.maven.builder.mojo.AbstractNativeMojo;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;
import org.uggeri.build.tools.compiler.Compiler;
        
/**
 *
 * @author fabio
 */
@Mojo(name = "process-sources", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ProcessSources extends AbstractNativeMojo {

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      final Set<String> headersExtensions = headersExtensions();
      loadSourceFiles(headersExtensions);
      loadTestSourceFiles(headersExtensions);
   }

   private Set<String> headersExtensions() {
      final Set<String> headersExtensions = new HashSet<>();
      for (Compiler compiler : getBuildContext().getCompilers().values()) {
         headersExtensions.addAll(compiler.supportedIncludes());
      }
      return headersExtensions;
   }

   /**
    * Carrega e valida a lista de arquivos a serem compilados
    */
   private void loadSourceFiles(final Set<String> headersExtensions) throws MojoExecutionException {
      try {
         final List<String> sourcesPathNames = new ArrayList<>();
         final List<File> sourceFiles = new ArrayList<>();
         loadSourcesFromDir(getBuildContext().getSourceDirectory(), sourceFiles, sourcesPathNames, headersExtensions);
         for (String sourceDir : getProject().getCompileSourceRoots()) {
            loadSourcesFromDir(sourceDir, sourceFiles, sourcesPathNames, headersExtensions);
         }
         setSources(sourcesPathNames);
         getBuildContext().setSourceFiles(sourceFiles);
      } catch (SourceFileScanException ex) {
         throw new MojoExecutionException("Error scaning source codes.", ex);
      }
   }

   private void loadSourcesFromDir(final String sourceDirPath, final List<File> sourceFiles, final List<String> sourcesPathNames, final Set<String> headersExtensions) throws SourceFileScanException, MojoExecutionException {
      List<File> filesFound;
      SourceFileScanner sourceScanner;
      File sourceBaseDir = new File(sourceDirPath);

      if (getLog().isDebugEnabled()) {
         getLog().debug("Scaning sources in " + sourceBaseDir.getAbsolutePath() + "...");
      } else {
         getLog().info("Scaning sources...");
      }

      if (getSources() == null && PackagingType.FILE.equals(getBuildContext().getPackagingType())) {
         throw new MojoExecutionException("Sources not found. Inform the sources to create in tags\n"
                 + "<SOURCES>\n"
                 + "<SOURCE>file1</SOURCE>\n"
                 + "<SOURCE>file2</SOURCE>\n"
                 + "...\n"
                 + "<SOURCE>fileN</SOURCE>\n"
                 + "</SOURCES>");
      }

      sourceScanner = new SourceFileScanner(sourceBaseDir, getSources(), getIncludeSources(), getExcludeSources());
      sourceScanner.setListener(new SourceFileScannerListenerImpl());
      filesFound = sourceScanner.scanFiles();

      /*
      * Percorre a lista de fontes, verificando se eh um arquivo, se ele
      * existe e se foi informado nas dependencias um compilador para seu
      * tipo
       */
      for (File fileSource : filesFound) {
         if (!fileSource.exists()) {
            throw new MojoExecutionException("File " + BuildUtil.canonicalPathName(fileSource) + " not found.");

         } else if (!fileSource.isFile()) {
            throw new MojoExecutionException(BuildUtil.canonicalPathName(fileSource) + " is not a file.");

         } else if ((PackagingType.FILE.equals(getBuildContext().getPackagingType())
                 || getBuildContext().isNeedCompilation(fileSource)
                 || getBuildContext().isCanPack(fileSource)) && !sourceFiles.contains(fileSource)) {
            sourceFiles.add(fileSource);
         } else if (headersExtensions.contains(BuildUtil.fileExtension(fileSource))) {
            getBuildContext().addHeaderFile(fileSource);
         } else if (getLog().isDebugEnabled()) {
            getLog().debug(BuildUtil.getRelativePath(sourceBaseDir, fileSource) + " skipped!");
         }
      }
      sourcesPathNames.addAll(BuildUtil.fileListToStringList(sourceBaseDir, sourceFiles));
   }

   private void loadTestSourceFiles(final Set<String> headersExtensions) throws MojoExecutionException {
      try {
         final List<File> sourceFiles = new ArrayList<>();
         loadTestSourcesFromDir(getBuildContext().getTestSourceDirectory(), sourceFiles, headersExtensions);
         for (String sourceDir : getProject().getTestCompileSourceRoots()) {
            loadTestSourcesFromDir(sourceDir, sourceFiles, headersExtensions);
         }
         getBuildContext().setTestSourceFiles(sourceFiles);
      } catch (SourceFileScanException ex) {
         throw new MojoExecutionException("Error scaning test sources.", ex);
      }
   }

   private void loadTestSourcesFromDir(final String sourceDir, final List<File> sourceFiles, final Set<String> headersExtensions) throws SourceFileScanException {
      File sourceBaseDir = new File(sourceDir);
      List<File> filesFound;
      SourceFileScanner sourceScanner;

      if (getLog().isDebugEnabled()) {
         getLog().debug("Scaning test sources in " + sourceBaseDir.getAbsolutePath() + "...");
      } else {
         getLog().info("Scaning test sources...");
      }
      /*
      * Carrega os nomes de todos arquivos encontrados no diretorio de fontes de teste
       */
      sourceScanner = new SourceFileScanner(sourceBaseDir);
      sourceScanner.setListener(new SourceFileScannerListenerImpl());
      filesFound = sourceScanner.scanFiles();

      /*
      * Percorre a lista de fontes, verificando se eh um arquivo compilavel
       */
      for (File fileSource : filesFound) {
         if (getBuildContext().isNeedCompilation(fileSource)) {
            if ((PackagingType.FILE.equals(getBuildContext().getPackagingType())
                    || getBuildContext().isNeedCompilation(fileSource)
                    || getBuildContext().isCanPack(fileSource)) && !sourceFiles.contains(fileSource)) {
               sourceFiles.add(fileSource);

            } else if (headersExtensions.contains(BuildUtil.fileExtension(fileSource))) {
               getBuildContext().addHeaderFile(fileSource);
            } else if (getLog().isDebugEnabled()) {
               getLog().debug(BuildUtil.getRelativePath(sourceBaseDir, fileSource) + " skipped!");
            }
         }
      }
   }

   private class SourceFileScannerListenerImpl implements SourceFileScannerListener {

      public SourceFileScannerListenerImpl() {
      }
      int level = 0;

      @Override
      public void dirScanStarted(File dir) {
         if (getLog().isDebugEnabled()) {
            getLog().debug(StringUtils.repeat(" ", level * 2) + dir.getName() + "...");
         }
         ++level;
      }

      @Override
      public void dirScanFinished(File dir) {
         --level;
      }

      @Override
      public void fileFound(File file) {
         if (getLog().isDebugEnabled()) {
            getLog().debug(StringUtils.repeat(" ", (level + 1) * 2) + file.getName() + " found.");
         }
      }

      @Override
      public void fileIncluded(File file) {
      }

      @Override
      public void fileDismissed(File file) {
         getLog().info("File not included in build: " + file.getAbsolutePath());
      }
   }

}
