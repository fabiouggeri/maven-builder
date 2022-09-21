/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo.compile;

import java.io.File;
import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 *
 * @author fabio_uggeri
 */
@Mojo(name = "test-compile", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.TEST)
public class TestCompileMojo extends AbstractCompileMojo {

   @Override
   protected List<File> getSourceFiles() {
      return getBuildContext().getTestSourceFiles();
   }

   @Override
   protected String getCompilationOutputDir() {
      return getBuildContext().getTestOutputDirectory();
   }

   @Override
   protected String getSourcesDir() {
      return getBuildContext().getTestSourceDirectory();
   }

   @Override
   protected void setOutputFiles(List<File> files) {
      getBuildContext().setTestSourceFiles(files);
   }

   @Override
   protected boolean isProcessingTestSources() {
      return true;
   }

   @Override
   protected boolean isSkipCompilation() {
      return isSkipTest();
   }
}
