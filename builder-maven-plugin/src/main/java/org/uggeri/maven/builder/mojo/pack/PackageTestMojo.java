/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo.pack;

import org.uggeri.build.tools.PackagingType;
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
 *
 * @author fabio_uggeri
 */
@Mojo(name = "package-test", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class PackageTestMojo extends AbstractPackageMojo {

   /**
    * Indica o nome do fonte principal para criar um executavel.
    */
   @Parameter(property = "testMainSource")
   private String testMainSource;

   @Parameter(defaultValue = "false", property = "mainSourcesTest")
   private boolean mainSourcesTest;

   @Override
   protected PackagingType packagingType() {
      return PackagingType.EXE;
   }

   @Override
   protected List<File> sourceFiles() {
      List<File> files = new ArrayList<>(getBuildContext().getTestSourceFiles());
      if (! files.isEmpty() && mainSourcesTest) {
         files.addAll(getBuildContext().getSourceFiles());
      }
      return files;
   }

   @Override
   protected Artifact createPackagingArtifact(PackagingType packagingType) {
      if (packagingType.isNeedCompile() && getBuildContext().getClassifier() != null && ! getBuildContext().getClassifier().trim().isEmpty()) {
         return getRepositorySystem().createArtifactWithClassifier(getProject().getGroupId(), "test-" + getProject().getArtifactId(), getProject().getVersion(), packagingType.toString(), getBuildContext().getClassifier());
      } else {
         return getRepositorySystem().createArtifact(getProject().getGroupId(), "test-" + getProject().getArtifactId(), getProject().getVersion(), packagingType.toString());
      }
   }

   @Override
   protected String outputDirectory() {
      return getBuildContext().getTestOutputDirectory();
   }

   @Override
   protected String mainSourceFileName() {
      return testMainSource;
   }

   @Override
   protected List<Dependency> getDependencies() {
      List<Dependency> deps = new ArrayList<>(getProject().getDependencies());
      deps.addAll(getProject().getTestDependencies());
      return deps;
   }

   @Override
   protected boolean isMainArtifact() {
      return false;
   }

   @Override
   protected boolean isProcessingTestSources() {
      return true;
   }

   @Override
   protected boolean isSkipPackaging() {
      return isSkipTest();
   }

   @Override
   protected boolean isIncludeTestDependency() {
      return true;
   }
}
