/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler.harbour;

import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.compiler.AbstractExternalCompiler;
import br.com.uggeri.build.tools.compiler.BatchCompilationRequest;
import br.com.uggeri.build.tools.compiler.CompilationRequest;
import br.com.uggeri.build.tools.compiler.SingleCompilationRequest;
import java.io.File;
import java.util.Map.Entry;
import java.util.*;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public class HarbourCompiler extends AbstractExternalCompiler {

   private static final Map<String, String> defaultOutputExtensions = new HashMap<String, String>();

   private static final Set<String> supportedIncludes = new HashSet<String>();

   static {
      defaultOutputExtensions.put("prg", "c");
      supportedIncludes.add("ch");
      supportedIncludes.add("api");
      supportedIncludes.add("h");
      supportedIncludes.add("hpp");
   }

   public HarbourCompiler() {
      super();
   }

   public HarbourCompiler(File executable) {
      super(executable);
   }

   /* Monta a linha de comando de compilacao do (x)Harbour no seguinte formato:
    * compilador.exe fonte.prg -i\\paths\\to\\includes -ooutFileName.c
    */
   @Override
   protected Commandline getCommandLine(SingleCompilationRequest request, File outFile) {
      StringBuilder commandStr = new StringBuilder(1024);
      Commandline commandLine;

      // Adiciona o pathName do executavel
      appendExecutablePathName(commandStr, "harbour.exe");

      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getStartOptions());
      } else {
         commandStr.append(" -m -n -w -gc2");
      }

      if (request.getOption("ppo").equalsIgnoreCase("true")) {
         commandStr.append(" -p"); // .append(Util.encloseFilePathName(Util.getRelativePath(source.getParentFile(), outFile.getParentFile())));
      }

      if (request.getOption("debug").equalsIgnoreCase("true")) {
         commandStr.append(" -b");
      }

      // Adiciona os defines
      appendDefines(commandStr, request);

      // Adiciona o pathName do codigo fonte
      commandStr.append(' ').append(request.getSource().getName());

      // Adiciona os paths dos includes
      appendIncludes(commandStr, request);

      // Adiciona o pathName do arquivo de saida
      if (outFile != null) {
         commandStr.append(" -o").append(BuildUtil.encloseFilePathName(BuildUtil.getRelativePath(request.getSource().getParentFile(), outFile)));
      }

      if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getEndOptions());
      }

      commandLine = BuildUtil.createCommandLine(commandStr.toString());
      commandLine.setWorkingDirectory(request.getSource().getParentFile());
      return commandLine;
   }

   @Override
   protected Commandline getBatchCommandLine(BatchCompilationRequest request, List<File> outFiles) {
      StringBuilder cmdStr = new StringBuilder(1024);
      Commandline commandLine;
      File sourceDir = request.getSources().get(0).getParentFile(); // Assume o diretorio do primeiro fonte como diretorio de origem
      File outDir = outFiles.get(0).getParentFile(); // Assume o diretorio do primeiro objeto como diretorio de destino
      String outPath;

      // Adiciona o pathName do executavel
      if (getExecutable().isDirectory()) {
         cmdStr.append("harbour.exe");
      } else {
         cmdStr.append(BuildUtil.encloseFilePathName(getExecutable().getName()));
      }

      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         cmdStr.append(' ').append(getToolConfig().getStartOptions());
      } else {
         cmdStr.append(" -m -n -w -gc2");
      }

      if (request.getOption("ppo").equalsIgnoreCase("true")) {
         cmdStr.append(" -p"); // .append(Util.encloseFilePathName(Util.getRelativePath(source.getParentFile(), outFile.getParentFile())));
      }

      if (request.getOption("debug").equalsIgnoreCase("true")) {
         cmdStr.append(" -b");
      }

      // Adiciona os defines
      appendDefines(cmdStr, request);

      // Adiciona os paths dos includes
      appendIncludes(cmdStr, request);

      // Adiciona o pathName do arquivo de saida
      outPath = BuildUtil.getRelativePath(sourceDir, outDir);
      if (! outPath.isEmpty()) {
         cmdStr.append(" -o").append(BuildUtil.encloseFilePathName(outPath + File.separator, false));
      }

      if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
         cmdStr.append(' ').append(getToolConfig().getEndOptions());
      }

      for (File source : request.getSources()) {
         cmdStr.append(' ').append(BuildUtil.encloseFilePathName(BuildUtil.getRelativePath(sourceDir, source), false));
      }
      commandLine = BuildUtil.createCommandLine(cmdStr.toString());
      commandLine.setWorkingDirectory(sourceDir);
      return commandLine;
   }

   /** Adiciona os defines a linha de comando */
   private void appendDefines(StringBuilder commandLine, CompilationRequest request) {
      for(Entry<String, String> define : request.getDefines().entrySet()) {
         if (! define.getKey().isEmpty()) {
            commandLine.append(" -d").append(define.getKey());
            if (define.getValue() != null && ! define.getValue().trim().isEmpty()) {
               commandLine.append('=').append(define.getValue().trim());
            }
         }
      }
   }

   /** Adiciona os include a linha de comando */
   private void appendIncludes(StringBuilder commandLine, CompilationRequest request) {
      boolean first = true;
      for(String incPath : request.getIncludesPaths()) {
         if (incPath != null && ! incPath.trim().isEmpty()) {
            if (first)  {
               commandLine.append(" -i").append(BuildUtil.encloseFilePathName(incPath.trim()));
               first = false;
            } else {
               commandLine.append(';').append(BuildUtil.encloseFilePathName(incPath.trim()));
            }
         }
      }
   }

   @Override
   public Collection<String> supportedTypes() {
      return HarbourCompiler.defaultOutputExtensions.keySet();
   }

   @Override
   public String getDefaultOutputExtension(String inputExtension) {
      String ext;
      if (inputExtension == null) {
         ext = "c";
      } else {
         ext = HarbourCompiler.defaultOutputExtensions.get(inputExtension.trim().toLowerCase());
         if (ext == null) {
            ext = "";
         }
      }
      return ext;
   }

   @Override
   public Collection<String> supportedIncludes() {
      return Collections.unmodifiableCollection(supportedIncludes);
   }

   @Override
   public boolean posCompilation(File srcFile, File outFile, CompilationRequest request) {
      if (request.getOption("ppo").equalsIgnoreCase("true")) {
         File ppoSourceFile = new File(srcFile.getParentFile(), BuildUtil.nameWithoutExtension(srcFile) + ".ppo");
         if (ppoSourceFile.exists()) {
            File ppoDestFile = new File(outFile.getParent(), ppoSourceFile.getName());
            if (! ppoSourceFile.equals(ppoDestFile)) {
               return BuildUtil.moveFile(ppoSourceFile, ppoDestFile, true);
            }
         }
      }
      return true;
   }

   @Override
   public boolean isBatchSupport() {
      return true;
   }
}
