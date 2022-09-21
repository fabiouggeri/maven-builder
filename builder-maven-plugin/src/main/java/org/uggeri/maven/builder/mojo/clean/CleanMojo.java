/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo.clean;

import org.uggeri.build.tools.PackagingType;
import org.uggeri.maven.builder.mojo.AbstractInitializeMojo;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.FileUtils;

/**
 * Clean target directorios
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractInitializeMojo {

   @Override
   public void execute() throws MojoExecutionException {
      try {
         final File cleanDir;
         initializeMojoConfigurations();
         defineDefaultDirectories();
         //      cleanCompiledObjects();
         //
         //      cleanPackagedObjects();
         getLog().info("Removing directory " + getBuildContext().getDirectory());
         cleanDir = new File(getBuildContext().getDirectory());
         if (cleanDir.isDirectory()) {
            FileUtils.cleanDirectory(cleanDir);
         }

         /* apaga os diretorios onde os artefatos sao extraidos */
         if (getMavenSession().getExecutionProperties().getProperty("all", "false").equalsIgnoreCase("true")) {
            deleteTemporaryArtifactDirectories();
         }
      } catch (IOException ex) {
         throw new MojoExecutionException("Error cleaning output objects directory.", ex);
      }
   }

   private void deleteTemporaryArtifactDirectories() throws IOException {
      Set<Artifact> dependencies = getProject().getDependencyArtifacts();
      if (dependencies != null) {
         for (Artifact artifact : dependencies) {
            if (artifact.getType().equalsIgnoreCase(PackagingType.INCLUDE.toString())) {
               final String dirPathName = pathToExtractArtifact(artifact);
               getLog().info("Cleaning dependencies directory " + dirPathName);
               FileUtils.deleteDirectory(new File(dirPathName));
            }
         }
      }
   }
}
