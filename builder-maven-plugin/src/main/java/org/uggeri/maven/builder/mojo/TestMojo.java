/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo;

import org.uggeri.build.tools.BuildUtil;
import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 *
 * @author fabio_uggeri
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class TestMojo extends AbstractNativeMojo {

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      if (isSkipTest()) {
         final File testFile = getBuildContext().getExecutableTestFile();
         if (testFile != null) {
            if (testFile.isFile()) {
               try {
                  final Commandline cmd;
                  final CaptureTestOutput stdoutConsumer = new CaptureTestOutput();
                  int exitCode;

                  getLog().info("Running unit tests...");
                  cmd = BuildUtil.createCommandLine(testFile.getAbsolutePath());
                  exitCode = CommandLineUtils.executeCommandLine(cmd, stdoutConsumer, stdoutConsumer);
                  getLog().info("");
                  if (exitCode == 0) {
                     getLog().info("Success in unit tests.");
                  } else {
                     throw new MojoExecutionException("Unit tests failed.");
                  }
               } catch (CommandLineException ex) {
                  throw new MojoFailureException("Error running unit tests.", ex);
               }
            } else {
               throw new MojoExecutionException(testFile + " is not a file.");
            }
         }
      }
   }

   private class CaptureTestOutput implements StreamConsumer {

      @Override
      public void consumeLine(String line) {
      }
   }
}
