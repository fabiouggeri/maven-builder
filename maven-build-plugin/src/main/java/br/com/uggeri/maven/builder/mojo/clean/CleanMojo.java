/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo.clean;

import br.com.uggeri.build.tools.PackagingType;
import br.com.uggeri.maven.builder.mojo.AbstractInitializeMojo;
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
         getLog().info("Removendo diretorio " + getBuildContext().getDirectory());
         cleanDir = new File(getBuildContext().getDirectory());
         if (cleanDir.isDirectory()) {
            FileUtils.cleanDirectory(cleanDir);
         }

         /* apaga os diretorios onde os artefatos sao extraidos */
         if (getMavenSession().getExecutionProperties().getProperty("all", "false").equalsIgnoreCase("true")) {
            deleteTemporaryArtifactDirectories();
         }
      } catch (IOException ex) {
         throw new MojoExecutionException("Erro ao limpar os diretorios de geracao dos objetos.", ex);
      }
   }

   private void deleteTemporaryArtifactDirectories() throws IOException {
      Set<Artifact> dependencies = getProject().getDependencyArtifacts();
      if (dependencies != null) {
         for (Artifact artifact : dependencies) {
            if (artifact.getType().equalsIgnoreCase(PackagingType.INCLUDE.toString())) {
               final String dirPathName = pathToExtractArtifact(artifact);
               getLog().info("Limpando diretorio de dependencias " + dirPathName);
               FileUtils.deleteDirectory(new File(dirPathName));
            }
         }
      }
   }
}
