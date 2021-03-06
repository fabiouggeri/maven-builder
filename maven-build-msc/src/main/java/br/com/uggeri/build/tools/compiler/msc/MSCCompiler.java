/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler.msc;

import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.compiler.AbstractExternalCompiler;
import br.com.uggeri.build.tools.compiler.BatchCompilationRequest;
import br.com.uggeri.build.tools.compiler.CompilationRequest;
import br.com.uggeri.build.tools.compiler.SingleCompilationRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.*;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public class MSCCompiler extends AbstractExternalCompiler {

   private static final Map<String, String> defaultOutputExtensions = new HashMap<String, String>();

   private static final Set<String> supportedIncludes = new HashSet<String>();

   static {
      defaultOutputExtensions.put("c", "obj");
      defaultOutputExtensions.put("cpp", "obj");
      supportedIncludes.add("h");
      supportedIncludes.add("hpp");
   }

   public MSCCompiler() {
      super();
   }

   public MSCCompiler(File executableFile) {
      super(executableFile);
   }

   @Override
   protected Commandline getCommandLine(SingleCompilationRequest request, File outFile) {
      StringBuilder commandStr = new StringBuilder(1024);
      Commandline commandLine;

      // Adiciona o pathName do executavel
      appendExecutablePathName(commandStr, "cl.exe");

      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getStartOptions());
      } else {
         // Inclui as opcoes default para compilacao
         if (BuildUtil.fileExtension(request.getSource()).equalsIgnoreCase("cpp")) {
            commandStr.append(" /EHsc");
         }
         commandStr.append(" /W3 /GA /Gs /Gd /Zl /Ox /c");
      }

      if (request.getOption("mode").equalsIgnoreCase("cpp")) {
         commandStr.append(" /TP");
      } else if (request.getOption("mode").equalsIgnoreCase("c")) {
         commandStr.append(" /TC");
      }

      if (request.getOption("debug").equalsIgnoreCase("true")) {
         commandStr.append(" /Zi");
      }

      if (request.getOption("mt", "true").equalsIgnoreCase("true")) {
         commandStr.append(" /MT");
      }

      if (request.getOption("verbose").equalsIgnoreCase("false")) {
         commandStr.append(" /nologo");
      }

      appendIncludes(commandStr, request);

      appendDefines(commandStr, request);

      // Adiciona o pathName do arquivo de saida
      if (outFile != null) {
         commandStr.append(" -Fo").append(BuildUtil.encloseFilePathName(BuildUtil.getRelativePath(request.getSource().getParentFile(), outFile)));
      }

      if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getEndOptions());
      }

      // Adiciona o pathName do codigo fonte
      commandStr.append(' ').append(request.getSource().getName());

      commandLine = BuildUtil.createCommandLine(commandStr.toString());
      commandLine.setWorkingDirectory(request.getSource().getParentFile());
      return commandLine;
   }

   @Override
   protected Commandline getBatchCommandLine(BatchCompilationRequest request, List<File> outFiles) {
      StringBuilder rspContent = new StringBuilder(1024);
      StringBuilder cmdStr = new StringBuilder(128);
      Commandline commandLine;
      File sourceDir = request.getSources().get(0).getParentFile(); // Assume o diretorio do primeiro fonte como diretorio de origem
      File outDir = outFiles.get(0).getParentFile(); // Assume o diretorio do primeiro objeto como diretorio de destino
      File rspFile;
      FileWriter writer = null;
      String outPath;

      try {
         rspFile = File.createTempFile("mscfiles", ".rsp", outDir);
         rspFile.deleteOnExit();
         if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
            rspContent.append(' ').append(getToolConfig().getStartOptions());
         } else {
            rspContent.append(" /EHsc /W3 /GA /Gs /Gd /Zl /arch:AVX2 /Ox /c");
         }

         if (request.getOption("mode").equalsIgnoreCase("cpp")) {
            rspContent.append(" /TP");
         } else if (request.getOption("mode").equalsIgnoreCase("c")) {
            rspContent.append(" /TC");
         }

         if (request.getOption("debug").equalsIgnoreCase("true")) {
            rspContent.append(" /Zi");
         }

         if (request.getOption("mt", "true").equalsIgnoreCase("true")) {
            rspContent.append(" /MT");
         }

         if (request.getOption("verbose").equalsIgnoreCase("false")) {
            rspContent.append(" /nologo");
         }

         appendIncludes(rspContent, request);

         appendDefines(rspContent, request);

         // Adiciona o pathName do diretorio de saida
         outPath = BuildUtil.getRelativePath(sourceDir, outDir);
         if (! outPath.isEmpty()) {
            rspContent.append(" /Fo").append(BuildUtil.encloseFilePathName(outPath + File.separator, false));
         }

         if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
            rspContent.append(' ').append(getToolConfig().getEndOptions());
         }

         // Adiciona o pathName do codigo fonte
         for (File source : request.getSources()) {
            rspContent.append('\n').append(BuildUtil.encloseFilePathName(BuildUtil.getRelativePath(sourceDir, source), false));
         }

         writer = new FileWriter(rspFile);
         writer.append(rspContent);
         writer.flush();
         // Adiciona o pathName do executavel
         if (getExecutable().isDirectory()) {
            cmdStr.append("cl.exe");
         } else {
            cmdStr.append(BuildUtil.encloseFilePathName(getExecutable().getName(), false));
         }
         cmdStr.append(" @").append(BuildUtil.encloseFilePathName(rspFile.getAbsolutePath(), false));

         commandLine = BuildUtil.createCommandLine(cmdStr.toString());
         commandLine.setWorkingDirectory(sourceDir);
         return commandLine;
      } catch (IOException ex) {
         return null;
      } finally {
         if (writer != null) {
            try {
               writer.close();
            } catch (IOException ex) {
            }
         }
      }
   }

   /** Adiciona os defines a linha de comando */
   private void appendDefines(StringBuilder commandLine, CompilationRequest request) {
      for(Entry<String, String> define : request.getDefines().entrySet()) {
         if (! define.getKey().isEmpty()) {
            commandLine.append(" /D").append(define.getKey());
            if (define.getValue() != null && ! define.getValue().trim().isEmpty()) {
               commandLine.append('=').append(define.getValue().trim());
            }
         }
      }
   }

   /** Adiciona os include a linha de comando */
   private void appendIncludes(StringBuilder commandLine, CompilationRequest request) {
      for(String incPath : request.getIncludesPaths()) {
         if (incPath != null && ! incPath.trim().isEmpty()) {
            commandLine.append(" /I").append(BuildUtil.encloseFilePathName(incPath.trim()));
         }
      }
   }

   @Override
   public String getDefaultOutputExtension(String inputExtension) {
      String ext;
      if (inputExtension == null) {
         ext = "obj";
      } else {
         ext = MSCCompiler.defaultOutputExtensions.get(inputExtension.trim().toLowerCase());
         if (ext == null) {
            ext = "";
         }
      }
      return ext;
   }

   @Override
   public Collection<String> supportedTypes() {
      return MSCCompiler.defaultOutputExtensions.keySet();
   }

   @Override
   public Collection<String> supportedIncludes() {
      return Collections.unmodifiableCollection(supportedIncludes);
   }

   @Override
   public boolean isBatchSupport() {
      return true;
   }
}
