/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.uggeri.build.tools.ArtifactDependency;
import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.PackagingType;
import org.uggeri.build.tools.log.Log;

/**
 *
 * @author fabio_uggeri
 */
public class ArtifactUtil {

   public final static String artifactName(Artifact artifact) {
      StringBuilder sb = new StringBuilder();
      sb.append(artifact.getArtifactId()).append('-');
      if (artifact.getVersion().isEmpty()) {
         sb.append(artifact.getBaseVersion());
      } else {
         sb.append(artifact.getVersion());
      }
      if (artifact.hasClassifier()) {
         sb.append('-').append(artifact.getClassifier());
      }
      sb.append('.').append(artifact.getType());
      return sb.toString();
   }

   public final static String artifactPomName(Artifact artifact) {
      StringBuilder sb = new StringBuilder();
      sb.append(artifact.getArtifactId()).append('-');
      if (artifact.getVersion().isEmpty()) {
         sb.append(artifact.getBaseVersion());
      } else {
         sb.append(artifact.getVersion());
      }
      if (artifact.hasClassifier()) {
         sb.append('-').append(artifact.getClassifier());
      }
      sb.append(".pom");
      return sb.toString();
   }

   static boolean areEqual(Artifact a, Dependency d) {
      return a.getGroupId().equals(d.getGroupId())
         && a.getArtifactId().equals(d.getArtifactId())
         && a.getVersion().equals(d.getVersion())
         && a.getType().equals(d.getType())
         && ((a.getClassifier() == null && d.getClassifier() == null) || a.getClassifier().equals(d.getClassifier()));
   }

   public static String pathToExtractArtifact(MavenBuildContext context, Artifact artifact) {
      return BuildUtil.pathToExtractArtifact(context.getExtractionDirectory().getAbsolutePath(), artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
   }
   
   public static void createArtifactExtractionDirectory(final Log log, final Artifact artifact, final File dirExtract) throws MojoExecutionException {
      /* Se diretorio nao existe, tenta criar... */
      if (!BuildUtil.createArtifactExtractionDirectory(log, artifact.getFile(), dirExtract)) {
         throw new MojoExecutionException("Error updating dependency " + artifact.getId() + ".");
      }
   }

   public static File createArtifactExtractionDirectory(MavenBuildContext context, final Log log, Artifact artifact) throws MojoExecutionException {
      final File dirExtract = new File(pathToExtractArtifact(context, artifact));
      createArtifactExtractionDirectory(log, artifact, dirExtract);
      return dirExtract;
   }

   public static Set<String> loadIncludesExtensions(MavenBuildContext context) {
      final Set<String> headersExtensions = new HashSet<String>();
      for (org.uggeri.build.tools.compiler.Compiler compiler : context.getCompilers().values()) {
         headersExtensions.addAll(compiler.supportedIncludes());
      }
      return headersExtensions;
   }

   public static List<String> unpackIncludes(List<Artifact> artifacts, MavenBuildContext context, boolean force, Log log) throws MojoExecutionException {
      final List<String> includes = new ArrayList<String>();
      final Set<String> headersExtensions = loadIncludesExtensions(context);
      for (Artifact artifact : artifacts) {
         /*
          * Dependencia do tipo INCLUDE significa que eh um conjunto de arquivos header
          * zipados. Entao, tem que extrair para um diretorio temporario e esse diretorio
          * deve ser informado no momento da compilacao.
          */
         if (artifact.getType().equalsIgnoreCase(PackagingType.INCLUDE.toString())) {
            File dirExtract = new File(pathToExtractArtifact(context, artifact));
            if (force) {
               try {
                  FileUtils.deleteDirectory(dirExtract);
                  log.debug("Directory " + dirExtract + " removed.");
               } catch (IOException ex) {
                  throw new MojoExecutionException("Error removing directory " + dirExtract, ex);
               }
            }
            if (!dirExtract.exists() || dirExtract.lastModified() < artifact.getFile().lastModified()) {
               createArtifactExtractionDirectory(log, artifact, dirExtract);
               log.info("Extraindo " + artifact.getId() + "...");
               if (!BuildUtil.unzip(artifact.getFile(), dirExtract.getAbsolutePath(), false, log)) {
                  throw new MojoExecutionException("Error unpacking include artifact " + artifact.getId() + ".");
               }
            }
            if (log.isDebugEnabled()) {
               for (String pathInc : BuildUtil.directoriesPathList(dirExtract)) {
                  log.debug("Add include path: " + pathInc);
               }
            }
            includes.addAll(BuildUtil.directoriesPathList(dirExtract));
            /*
             * Se for dependencia de um artefato do tipo CH, H, API ou HPP, entao tem que
             * copiar o artefato para um diretorio temporario com seu nome original e informar
             * esse path no momento da compilacao.
             */
         } else if (artifact.getType().equalsIgnoreCase(PackagingType.FILE.name())) {
            if (headersExtensions.contains(BuildUtil.fileExtension(artifact.getArtifactId()))) {
               File dirExtract = createArtifactExtractionDirectory(context, log, artifact);
               File destFile = new File(dirExtract, artifact.getArtifactId());
               try {
                  if (!destFile.exists() || artifact.getFile().lastModified() > destFile.lastModified()) {
                     log.debug("Copying file " + artifact.getFile().getAbsolutePath() + " to " + destFile.getAbsolutePath() + ".");
                     FileUtils.copyFile(artifact.getFile(), destFile);
                  }
                  if (log.isDebugEnabled()) {
                     log.debug("Add include path: " + dirExtract.getAbsolutePath());
                  }
                  includes.add(dirExtract.getAbsolutePath());
               } catch (IOException ex) {
                  log.debug("Error copying file " + artifact.getFile().getAbsolutePath() + " to " + destFile.getAbsolutePath() + ".\nException: " + ex.getMessage());
                  throw new MojoExecutionException("Error configuring dependencies.", ex);
               }
            }
         }
      }
      for (File headerFile : context.getHeaderFiles()) {
         if (headerFile.isDirectory()) {
            if (log.isDebugEnabled()) {
               log.debug("Add include path: " + headerFile.getAbsolutePath());
            }
            includes.add(headerFile.getAbsolutePath());
         } else {
            if (log.isDebugEnabled()) {
               log.debug("Add include path: " + headerFile.getParentFile().getAbsolutePath());
            }
            includes.add(headerFile.getParentFile().getAbsolutePath());
         }
      }
      return includes;
   }
   
   public static void unzipArtifact(MavenBuildContext context, Artifact artifact, boolean force, Log log) throws MojoExecutionException {
      final File dirExtract = new File(pathToExtractArtifact(context, artifact));
      if (force) {
         try {
            FileUtils.deleteDirectory(dirExtract);
            log.debug("Directory " + dirExtract + " removed.");
         } catch (IOException ex) {
            throw new MojoExecutionException("Error removing directory " + dirExtract, ex);
         }
      }
      if (!dirExtract.exists() || dirExtract.lastModified() < artifact.getFile().lastModified()) {
         createArtifactExtractionDirectory(log, artifact, dirExtract);
         log.info("Extraindo " + artifact.getId() + "...");
         if (!BuildUtil.unzip(artifact.getFile(), dirExtract.getAbsolutePath(), false, log)) {
            throw new MojoExecutionException("Error upacking files of artifact " + artifact.getId() + ".");
         }
      }
      
   }
   
   public static Set<ArtifactDependency> artifactsDependencies(MavenProject project) {
      Set<ArtifactDependency> dependencies = new HashSet<ArtifactDependency>();
      for (Artifact artifact : (Set<Artifact>) project.getDependencyArtifacts()) {
         dependencies.add(new ArtifactDependency(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getClassifier(), artifact.getScope()));
      }
      return dependencies;
   }
}
