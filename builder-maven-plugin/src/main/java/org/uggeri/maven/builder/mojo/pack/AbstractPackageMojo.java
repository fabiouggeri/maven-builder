/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo.pack;

import org.uggeri.maven.builder.MavenLogWrapper;
import org.uggeri.build.tools.PackagingType;
import org.uggeri.build.tools.Version;
import org.uggeri.build.tools.packer.PackagingRequest;
import org.uggeri.build.tools.packer.PackagingRequestImpl;
import org.uggeri.build.tools.packer.PackagingResult;
import org.uggeri.build.tools.packer.Packer;
import org.uggeri.maven.builder.mojo.AbstractNativeMojo;
import org.uggeri.maven.builder.mojo.ArtifactUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.util.IOUtil;

public abstract class AbstractPackageMojo extends AbstractNativeMojo {

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      if (!isSkipPackaging()) {
         PackagingType packagingType = PackagingType.valueOf(getProject().getPackaging().toUpperCase());

         if (!packagingType.isNeedPackage()) {
            noPackageExecution();

         } else if (!getBuildContext().getPackers().isEmpty()) {
            packageExecution();

         } else {
            throw new MojoExecutionException("Packer not found in configuration.");
         }
      }
   }

   private Artifact createArtifact(File sourceFile) {
      Artifact artifact;
      if (getBuildContext().getClassifier() != null && !getBuildContext().getClassifier().replace("-", "").isEmpty()) {
         artifact = getRepositorySystem().createArtifactWithClassifier(getProject().getGroupId(), sourceFile.getName(), getProject().getVersion(), getProject().getPackaging(), getBuildContext().getClassifier());
      } else {
         artifact = getRepositorySystem().createArtifact(getProject().getGroupId(), sourceFile.getName(), getProject().getVersion(), getProject().getPackaging());
      }
      artifact.setFile(sourceFile);
      return artifact;
   }

   private void noPackageExecution() throws MojoExecutionException {
      for (File sourceFile : getBuildContext().getSourceFiles()) {
         Artifact artifact;
         artifact = createArtifact(sourceFile);
         generatePom(artifact);
         getProject().addAttachedArtifact(artifact);
      }
   }

   private void generatePom(Artifact artifact) throws MojoExecutionException {
      FileWriter fw = null;
      try {
         ProjectArtifactMetadata metadata;
         File tempFile = File.createTempFile("NativeBuildPom", ".pom");
         tempFile.deleteOnExit();

         Model model = new Model();
         model.setModelVersion("4.0.0");
         model.setGroupId(artifact.getGroupId());
         model.setArtifactId(artifact.getArtifactId());
         model.setVersion(artifact.getVersion());
         model.setPackaging(artifact.getType());
         model.setDescription("POM created by maven-native-builder:package");
         fw = new FileWriter(tempFile);
         tempFile.deleteOnExit();
         new MavenXpp3Writer().write(fw, model);
         metadata = new ProjectArtifactMetadata(artifact, tempFile);
         artifact.addMetadata(metadata);
      } catch (IOException e) {
         throw new MojoExecutionException("Error creating temporary pom: " + e.getMessage(), e);
      } finally {
         IOUtil.close(fw);
      }
   }

   protected abstract PackagingType packagingType();

   protected abstract List<File> sourceFiles();

   protected abstract List<Dependency> getDependencies();

   protected abstract Artifact createPackagingArtifact(PackagingType packagingType);

   protected abstract String outputDirectory();

   protected abstract String mainSourceFileName();

   protected abstract boolean isMainArtifact();

   protected abstract boolean isSkipPackaging();

   protected abstract boolean isIncludeTestDependency();

   private void packageExecution() throws MojoExecutionException {
      final Artifact artifact;
      final PackagingRequest request = new PackagingRequestImpl();
      final PackagingResult result;
      final PackagingType packagingType = packagingType();
      final List<File> inputFiles;
      final String artifactName;
      final boolean verbose = getVerbose().equalsIgnoreCase("true") || getVerbose().equalsIgnoreCase("full");
      final Packer packer;

      packer = getBuildContext().getPackers().get(packagingType.toString());
      if (packer != null) {
         inputFiles = sourceFiles();
         artifact = createPackagingArtifact(packagingType);
         artifactName = ArtifactUtil.artifactName(artifact);
         if (!inputFiles.isEmpty()) {
            getLog().info("Gerando " + artifactName + "...");
            request.setProperties(getExecutionProperties());
            request.setOutputDir(outputDirectory());
            request.setOutputFileName(artifactName);
            request.setMainSourceFileName(mainSourceFileName());
            request.setVersion(Version.parse(artifact.getVersion()));
            request.setLog(new MavenLogWrapper(getLog()));
            request.setLibraries(listLibraries(packer, inputFiles, isIncludeTestDependency()));
            request.setSources(inputFiles);
            request.setEnvironmentVariables(getBuildContext().getEnvironmentVariables());
            request.setPathSeparator(getPathSeparator());
            result = packer.execute(request);
            if (!result.isSuccessful() || getLog().isDebugEnabled() || verbose || hasWarnings(result)) {
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
            if (!result.isSuccessful() || !result.getOutputFile().exists()) {
               throw new MojoExecutionException("Falha ao criar " + artifactName + ".");
            }
            artifact.setFile(result.getOutputFile());
            if (isMainArtifact()) {
               getProject().setArtifact(artifact);
            }
            /* Se esta gerando o executavel de teste, entao guarda
             o arquivo para posterior execucao dos testes */
            if (isProcessingTestSources()) {
               getBuildContext().setExecutableTestFile(result.getOutputFile());
            }
         } else {
            getLog().warn("No source found to build " + artifactName + ".");
         }
      }
   }
}
