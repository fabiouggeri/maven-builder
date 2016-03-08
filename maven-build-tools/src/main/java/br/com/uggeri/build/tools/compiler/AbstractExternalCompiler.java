/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.tools.compiler;

import br.com.uggeri.build.command.CaptureConsumer;
import br.com.uggeri.build.tools.AbstractExternalTool;
import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.ToolType;
import br.com.uggeri.build.tools.log.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public abstract class AbstractExternalCompiler extends AbstractExternalTool<CompilationResult, CompilationRequest> implements Compiler {

   public AbstractExternalCompiler() {
      super();
   }

   public AbstractExternalCompiler(File executable) {
      super(executable);
   }

   @Override
   public CompilationResult execute(CompilationRequest request) {
      if (request.isBatch()) {
         return compileBatch((BatchCompilationRequest)request);
      } else {
         return compileSingle((SingleCompilationRequest)request);
      }
   }

   private CompilationResult compileSingle(SingleCompilationRequest request) {
      File outFile = getOutputFile(request.getSource(), request);
      final StringBuilder path = new StringBuilder(256);
      final CaptureConsumer stdoutConsumer = new CaptureConsumer();
      final Log log = request.getLog();
      int exitCode = 1;
      Commandline command;

      /* Se nao estiver forcando a compilacao verifica se o arquivo objeto ja existe e esta atualizado.
         Se estiver, retorna sem executar a compilacao */
      if (! request.isForce()&& outFile.exists() && request.getSource().lastModified() < outFile.lastModified()) {
         return new CompilationResultImpl(outFile);
      }

      command = getCommandLine(request, outFile);
      if (command != null) {
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
            log.debug("Variavel PATH: " + path.toString());
            log.debug("Path execucao: " + command.getWorkingDirectory());
            log.debug("Comando      : " + command.toString());
         }
         try {
            FileUtils.forceDelete(outFile);
            try {
               exitCode = CommandLineUtils.executeCommandLine(command, stdoutConsumer, stdoutConsumer);
               log.debug("Codigo saida: " + exitCode);
               if (exitCode != 0 || ! outFile.exists()) {
                  outFile = null;
               }
            } catch (CommandLineException ex) {
               log.debug("Exception: " + ex.getMessage(), ex);
               stdoutConsumer.consumeLine("Erro executando compilador externo: " + ex.getMessage());
               if (ex.getCause() != null) {
                  stdoutConsumer.consumeLine("   Causa: " + ex.getCause().getMessage());
               }
               outFile = null;
            }
         } catch(IOException ex) {
            log.debug("Exception: " + ex.getMessage());
            outFile = null;
         }
      } else {
         outFile = null;
         log.error("Nao foi possivel gerar a linha de comando!");
      }
      return new CompilationResultImpl(outFile, stdoutConsumer.getLines(), exitCode);
   }

   private List<File> getOutputFiles(List<File> sourceFiles, AbstractCompilationRequest config) {
      final List<File> outFiles = new ArrayList<File>(sourceFiles.size());
      for (File source : sourceFiles) {
         File outFile = getOutputFile(source, config);
         if (outFile != null) {
            outFiles.add(outFile);
         }
      }
      return outFiles;
   }

   private void removeUnnecessaryCompilationSources(List<File> sourceFiles, List<File> outFiles) {
      int i = 0;
      for (Iterator<File> itSource = sourceFiles.iterator(); itSource.hasNext(); ) {
         File source = itSource.next();
         File outFile = outFiles.get(i++);
         if (outFile != null && outFile.exists() && source.lastModified() < outFile.lastModified()) {
            outFiles.add(outFile);
            itSource.remove();
         }
      }
   }

   private CompilationResult compileBatch(BatchCompilationRequest request) {
      final List<File> outFiles = getOutputFiles(request.getSources(), request);
      final StringBuilder path = new StringBuilder(256);
      final CaptureConsumer stdoutConsumer = new CaptureConsumer();
      final Log log = request.getLog();
      int exitCode = 1;
      Commandline command;

      /* Se nao estiver forcando a compilacao verifica se o arquivo objeto ja existe e esta atualizado.
         Se estiver, retorna sem executar a compilacao */
      if (! request.isForce()) {
         removeUnnecessaryCompilationSources(request.getSources(), outFiles);
         /* Se sources estiver vazio eh porque achou todos os objetos compilados atualizados */
         if (request.getSources().isEmpty()) {
            return new CompilationResultImpl(outFiles);
         }
      }

      command = getBatchCommandLine(request, outFiles);
      if (command != null) {
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
         log.debug("Path Execucao: " + command.getWorkingDirectory());
         try {
            /* Apaga arquivos objetos a serem compilados */
            for (File outFile : outFiles) {
               FileUtils.forceDelete(outFile);
            }
            try {
               exitCode = CommandLineUtils.executeCommandLine(command, stdoutConsumer, stdoutConsumer);
               log.debug("Comando: " + command.toString() + "\n Codigo saida: " + exitCode);
               if (exitCode != 0 || ! filesExists(outFiles)) {
                  outFiles.clear();
               }
            } catch (CommandLineException ex) {
               log.debug("Exception: " + ex.getMessage());
               outFiles.clear();
            }
         } catch(IOException ex) {
            log.debug("Exception: " + ex.getMessage());
            outFiles.clear();
         }
      } else {
         outFiles.clear();
         log.error("Nao foi possivel gerar a linha de comando!");
      }
      return new CompilationResultImpl(outFiles, stdoutConsumer.getLines(), exitCode);
   }

   @Override
   public File getOutputFile(File source, CompilationRequest request) {
      String outFileName = BuildUtil.nameWithoutExtension(source);
      String outFileExt;
      String sourceDir;
      File outputDir;

      if (BuildUtil.isChildFile(request.getOutputDir(), source)) {
         outputDir = source.getParentFile();
      } else {
         sourceDir = BuildUtil.getRelativePath(request.getSourceDirectory(), source.getParentFile());
         outputDir = new File(BuildUtil.removeLastPathSeparator(request.getOutputDir()), sourceDir);
      }

      /* Verifica se o diretorio de saida ja existe. Se nao existae, entao cria. */
      if (! outputDir.isDirectory()) {
         outputDir.mkdirs();
      }

      // Verifica a extensao
      if (request.getOutputFileExtension() == null) {
         outFileExt = getDefaultOutputExtension(BuildUtil.fileExtension(source));
      } else {
         outFileExt = request.getOutputFileExtension().trim();
         if (outFileExt.isEmpty()) {
            outFileExt = getDefaultOutputExtension(BuildUtil.fileExtension(source));
         }
      }
      return new File(outputDir, outFileName + '.' + outFileExt);
   }

   @Override
   public String getDefaultOutputExtension() {
      return getDefaultOutputExtension(null);
   }

   @Override
   public boolean preCompilation(File srcFile, CompilationRequest request) {
      return true;
   }

   @Override
   public boolean posCompilation(File srcFile, File outFile, CompilationRequest request) {
      return true;
   }


   private boolean filesExists(List<File> files) {
      for (File file : files) {
         if (! file.exists()) {
            return false;
         }
      }
      return true;
   }

   @Override
   public ToolType getToolType() {
      return ToolType.COMPILER;
   }

   protected abstract Commandline getCommandLine(SingleCompilationRequest request, File outFile);

   protected abstract Commandline getBatchCommandLine(BatchCompilationRequest request, List<File> outFiles);
}
