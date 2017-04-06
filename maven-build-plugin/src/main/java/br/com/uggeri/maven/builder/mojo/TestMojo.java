/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo;

import br.com.uggeri.build.tools.BuildUtil;
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

                  getLog().info("Executando testes unitários...");
                  cmd = BuildUtil.createCommandLine(testFile.getAbsolutePath());
                  exitCode = CommandLineUtils.executeCommandLine(cmd, stdoutConsumer, stdoutConsumer);
                  getLog().info("");
                  if (exitCode == 0) {
                     getLog().info("Sucesso na execucao dos testes unitarios.");
                  } else {
                     throw new MojoExecutionException("Falha nos testes unitarios.");
                  }
               } catch (CommandLineException ex) {
                  throw new MojoFailureException("Erro ao executar os testes.", ex);
               }
            } else {
               throw new MojoExecutionException(testFile + " não é um arquivo.");
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
