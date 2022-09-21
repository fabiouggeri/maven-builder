/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.custom.compiler;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.compiler.AbstractExternalCompiler;
import org.uggeri.build.tools.compiler.BatchCompilationRequest;
import org.uggeri.build.tools.compiler.SingleCompilationRequest;
import org.uggeri.build.tools.custom.CustomCommandLineBuilder;
import org.uggeri.build.tools.custom.vars.ListVariable;
import org.uggeri.build.tools.custom.vars.MapVariable;
import org.uggeri.build.tools.custom.vars.StringVariable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.uggeri.build.tools.OptionsFile;
import org.uggeri.build.tools.compiler.CompilationRequest;

/**
 *
 * @author ADMIN
 */
public class CustomCompiler extends AbstractExternalCompiler {

//   public static void main(String[] args) {
//      new CustomCompiler().teste();
//   }
//   
//   private void teste() {
//      SingleCompilationRequest req = new SingleCompilationRequest(new File("C:\\temp\\base64_openssl.c"));
//      Map<String, String> defines = new HashMap<>();
//      List<String> includesPaths = new ArrayList<>();
//      Properties prop = new Properties();
//      defines.put("MEDIATOR", "5.2.2.0");
//      defines.put("HARBOUR", "3.2.0");
//      includesPaths.add("c:\\temp;C:\\tools\\harbour\\3.2.0\\include");
//      prop.setProperty("debug", "true");
//      prop.setProperty("m64", "a");
//      req.setDefines(defines);
//      req.setIncludesPaths(includesPaths);
//      req.setOutputDir("c:\\temp");
//      req.setOutputFileExtension("obj");
//      req.setProperties(prop);
//      req.setSourceDirectory(new File("c:\\temp"));
//      createVariables(req, new File("c:\\temp\\base64_openssl.obj"));
//      System.out.println(BuildUtil.createCommandLine(buildCommandLine("gcc -c #{defines, '-d', ' -d', '='} #{inputFile} #{?m64='b','-m32'} #{?debug='true','-D'} #{includesPaths, '-I'} -o#{outputFile}", req)));
//   }
          
   public CustomCompiler() {
      super();
   }

   @Override
   protected Commandline getCommandLine(SingleCompilationRequest request, File outFile) {
      Commandline commandLine; 
      CustomCommandLineBuilder cmdBuilder = new CustomCommandLineBuilder();
      createVariables(cmdBuilder, request, outFile);
      createOptionsFile(cmdBuilder, request, getToolConfig().getOptionsFile());
      commandLine = BuildUtil.createCommandLine(cmdBuilder.buildCommandLine(getToolConfig().getCommandLine(), this, request));
      commandLine.setWorkingDirectory(request.getSource().getParentFile());
      return commandLine;
   }

   @Override
   protected Commandline getBatchCommandLine(BatchCompilationRequest request, List<File> outFiles) {
      File sourceDir; 
      Commandline commandLine; 
      CustomCommandLineBuilder cmdBuilder = new CustomCommandLineBuilder();
      createBatchVariables(cmdBuilder, request, outFiles);
      createOptionsFile(cmdBuilder, request, getToolConfig().getOptionsFile());
      sourceDir = request.getSources().get(0).getParentFile(); // Assume o diretorio do primeiro fonte como diretorio de origem
      commandLine = BuildUtil.createCommandLine(cmdBuilder.buildCommandLine(getToolConfig().getCommandLine(), this, request));
      commandLine.setWorkingDirectory(sourceDir);
      return commandLine;
   }

   @Override
   public Collection<String> supportedTypes() {
      if (getToolConfig().getSourceTypes() != null) {
         return Arrays.asList(getToolConfig().getSourceTypes());
      } else {
         return Collections.emptyList();
      }
   }

   @Override
   public boolean isBatchSupport() {
      return getToolConfig().isBatchSupport();
   }

   @Override
   public String getDefaultOutputExtension(String inputExtension) {
      String outExt = getToolConfig().getOutputType();
      if (outExt == null || outExt.isEmpty()) {
         outExt = "obj";
      }
      return outExt;
   }

   @Override
   public Collection<String> supportedIncludes() {
      if (getToolConfig().getIncludeTypes() != null) {
         return Arrays.asList(getToolConfig().getIncludeTypes());
      } else {
         return Collections.emptyList();
      }
   }

   private void createVariables(CustomCommandLineBuilder cmdBuilder, SingleCompilationRequest request, File outFile) {
      if (getExecutable() != null) {
         cmdBuilder.putVariable("toolExe", new StringVariable(getExecutable().getAbsolutePath()));
      }
      cmdBuilder.putVariable("inputFile", new StringVariable(BuildUtil.normalizePath(request.getSource().getAbsolutePath(), request.getPathSeparator())));
      cmdBuilder.putVariable("inputFiles", new ListVariable(BuildUtil.normalizePath(Arrays.asList(request.getSource().getAbsolutePath()), request.getPathSeparator())));
      cmdBuilder.putVariable("libsPaths", new ListVariable(new ArrayList<String>()));
      cmdBuilder.putVariable("includesPaths", new ListVariable(BuildUtil.normalizePath(request.getIncludesPaths(), request.getPathSeparator())));
      cmdBuilder.putVariable("outputFile", new StringVariable(BuildUtil.normalizePath(outFile.getAbsolutePath(), request.getPathSeparator())));
      cmdBuilder.putVariable("outputFiles", new ListVariable(BuildUtil.normalizePath(Arrays.asList(outFile.getAbsolutePath()), request.getPathSeparator())));
      cmdBuilder.putVariable("defines", new MapVariable(request.getDefines()));
   }
   
   private void createBatchVariables(CustomCommandLineBuilder cmdBuilder, BatchCompilationRequest request, List<File> outFiles) {
      if (getExecutable() != null) {
         cmdBuilder.putVariable("toolExe", new StringVariable(getExecutable().getAbsolutePath()));
      }
      cmdBuilder.putVariable("inputFile", new StringVariable(BuildUtil.normalizePath(request.getSources().get(0).getAbsolutePath(), request.getPathSeparator())));
      cmdBuilder.putVariable("inputFiles", new ListVariable(BuildUtil.normalizePath(BuildUtil.toStringList(request.getSources(), false), request.getPathSeparator())));
      cmdBuilder.putVariable("libsPaths", new ListVariable(new ArrayList<String>()));
      cmdBuilder.putVariable("includesPaths", new ListVariable(BuildUtil.normalizePath(request.getIncludesPaths(), request.getPathSeparator())));
      cmdBuilder.putVariable("outputFile", new StringVariable(BuildUtil.normalizePath(outFiles.get(0).getAbsolutePath(), request.getPathSeparator())));
      cmdBuilder.putVariable("outputFiles", new ListVariable(BuildUtil.normalizePath(BuildUtil.toStringList(outFiles, false), request.getPathSeparator())));
      cmdBuilder.putVariable("defines", new MapVariable(request.getDefines()));
   }

   @Override
   protected String getEnvPath() {
      return System.getenv("PATH");
   }

   private void createOptionsFile(CustomCommandLineBuilder cmdBuilder, CompilationRequest request, OptionsFile optionsFile) {
      if (optionsFile != null && optionsFile.getPathName() != null && ! StringUtils.isEmpty(optionsFile.getContent())) {
         try {
            final String fileContent = cmdBuilder.buildCommandLine(optionsFile.getContent(), this, request);
            FileUtils.fileWrite(optionsFile.getPathName(), fileContent);
         } catch (IOException ex) {
            request.getLog().error("Erro ao criar o arquivo de opcoes '" + optionsFile.getPathName() + "'.", ex);
         }
      }
   }
}
