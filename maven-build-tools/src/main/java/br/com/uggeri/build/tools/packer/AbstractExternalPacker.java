/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer;

import br.com.uggeri.build.command.CaptureConsumer;
import br.com.uggeri.build.tools.AbstractExternalTool;
import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.ToolType;
import br.com.uggeri.build.tools.log.Log;
import java.io.File;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public abstract class AbstractExternalPacker extends AbstractExternalTool<PackagingResult, PackagingRequest> implements Packer {

   public AbstractExternalPacker() {
      super();
   }

   public AbstractExternalPacker(File executable) {
      super(executable);
   }

   @Override
   public PackagingResult execute(PackagingRequest request) {
      File outFile = outputFile(request);
      int exitCode = 1;
      final StringBuilder path = new StringBuilder();
      final CaptureConsumer stdoutConsumer = new CaptureConsumer();
      final Log log = request.getLog();


      if (outFile.exists()) {
         outFile.delete();
      }
      log.debug("Montando " + outFile + "...");
      Commandline command = getCommandLine(request, outFile);
      if (command != null) {
         path.append(getEnvPath());
         if (getExecutable() != null) {
            if (getExecutable().isDirectory()) {
               path.append(getExecutable().getAbsolutePath());
            } else {
               path.append(getExecutable().getParentFile().getAbsolutePath());
            }
         }
         if (getExecutionDependencies() != null && ! getExecutionDependencies().isEmpty()) {
            path.append(File.pathSeparator).append(BuildUtil.filesPath(getExecutionDependencies()));
         }
         command.addEnvironment("PATH", path.toString());
         addConfiguredEnvironmentVariables(command, request.getEnvironmentVariables());
         debugEnvironmentVariables(log, command);
         if (log.isDebugEnabled()) {
            log.debug("Path execucao: " + command.getWorkingDirectory());
            log.debug("Comando      : " + command.toString());
         }
         try {
            exitCode = CommandLineUtils.executeCommandLine(command, stdoutConsumer, stdoutConsumer);
            log.debug("Codigo saida: " + exitCode);
            if (!outFile.exists()) {
               outFile = null;
            }
         } catch (CommandLineException ex) {
            log.debug("Exception: " + ex.getMessage(), ex);
            stdoutConsumer.consumeLine("Erro executando empacotador externo: " + ex.getMessage());
            if (ex.getCause() != null) {
               stdoutConsumer.consumeLine("   Causa: " + ex.getCause().getMessage());
            }
            outFile = null;
         }
      } else {
         log.error("Erro ao gerar a linha de comando!");
         outFile = null;
      }
      return new PackagingResultImpl(outFile, stdoutConsumer.getLines(), exitCode);
   }

   private File outputFile(PackagingRequest request) {
      final String trimFileName = request.getOutputFileName().trim();
      final String outFileName = BuildUtil.removeExtension(trimFileName);
      String fileExt = BuildUtil.fileExtension(trimFileName);

      // Verifica a extensao
      if (request.getOutputFileExtension() == null) {
         if (fileExt == null || fileExt.isEmpty()) {
            fileExt = getDefaultOutputExtension();
         }
      } else {
         fileExt = request.getOutputFileExtension().trim();
         if (fileExt.isEmpty()) {
            fileExt = getDefaultOutputExtension();
         }
      }
      return new File(BuildUtil.removeLastPathSeparator(request.getOutputDir()) + File.separatorChar + outFileName + '.' + fileExt);
   }

   @Override
   public ToolType getToolType() {
      return ToolType.PACKER;
   }

   protected abstract Commandline getCommandLine(PackagingRequest config, File outFile);

}
