/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo.pack;

import br.com.uggeri.build.tools.PackagingType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Package object files in appropriate format
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PackageMojo extends AbstractPackageMojo {

   /**
    * Indica o nome do fonte principal para criar um executavel.
    */
   @Parameter(property = "mainSource")
   private String mainSource;

   @Override
   protected PackagingType packagingType() {
      return PackagingType.valueOf(getProject().getPackaging().toUpperCase());
   }

   @Override
   protected List<File> sourceFiles() {
      return new ArrayList<File>(getBuildContext().getSourceFiles());
   }

   @Override
   protected Artifact createPackagingArtifact(PackagingType packagingType) {
      if (packagingType.isNeedCompile() && getBuildContext().getClassifier() != null && ! getBuildContext().getClassifier().trim().isEmpty()) {
         return getRepositorySystem().createArtifactWithClassifier(getProject().getGroupId(), getProject().getArtifactId(), getProject().getVersion(), getProject().getPackaging(), getBuildContext().getClassifier());
      } else {
         return getRepositorySystem().createArtifact(getProject().getGroupId(), getProject().getArtifactId(), getProject().getVersion(), getProject().getPackaging());
      }
   }

   @Override
   protected String outputDirectory() {
      return getBuildContext().getOutputDirectory();
   }

   @Override
   protected String mainSourceFileName() {
      return mainSource;
   }

   @Override
   protected List<Dependency> getDependencies() {
      return getProject().getDependencies();
   }

   @Override
   protected boolean isMainArtifact() {
      return true;
   }

   @Override
   protected boolean isSkipPackaging() {
      return false;
   }
}
