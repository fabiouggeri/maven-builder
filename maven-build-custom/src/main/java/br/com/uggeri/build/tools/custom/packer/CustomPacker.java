/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.packer;

import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.custom.CustomCommandLineBuilder;
import br.com.uggeri.build.tools.custom.vars.ListVariable;
import br.com.uggeri.build.tools.custom.vars.StringVariable;
import br.com.uggeri.build.tools.custom.vars.VariableDefinition;
import br.com.uggeri.build.tools.packer.AbstractExternalPacker;
import br.com.uggeri.build.tools.packer.PackagingRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author Fabio
 */
public class CustomPacker extends AbstractExternalPacker {

   private final Map<String, VariableDefinition> variables = new HashMap<String, VariableDefinition>();

   public CustomPacker() {
      super();
   }

   @Override
   protected Commandline getCommandLine(PackagingRequest request, File outFile) {
      Commandline commandLine;
      CustomCommandLineBuilder cmdBuilder = new CustomCommandLineBuilder();
      createVariables(cmdBuilder, request, outFile);
      commandLine = BuildUtil.createCommandLine(cmdBuilder.buildCommandLine(getToolConfig().getCommandLine(), request));
      commandLine.setWorkingDirectory(FileUtils.getPath(outFile.getAbsolutePath()));
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
   public String getDefaultOutputExtension() {
      String outExt = getToolConfig().getOutputType();
      if (outExt == null || outExt.isEmpty()) {
         outExt = "exe";
      }
      return outExt;
   }

   @Override
   public Collection<String> getSupportedPackagings() {
      if (getToolConfig().getPackagingTypes() != null) {
         return Arrays.asList(getToolConfig().getPackagingTypes());
      } else {
         return Collections.emptyList();
      }
   }

   private void createVariables(CustomCommandLineBuilder cmdBuilder, PackagingRequest request, File outFile) {
      final String mainSourceName;
      if (request.getMainSourceFileName() != null) {
         mainSourceName = BuildUtil.removePath(BuildUtil.removePath(BuildUtil.removeExtension(request.getMainSourceFileName()),'/'),'\\');
      } else {
         mainSourceName = "main";
      }
      cmdBuilder.putVariable("mainSourceFile", new StringVariable(request.getMainSourceFileName()));
      cmdBuilder.putVariable("inputFiles", new ListVariable(BuildUtil.toStringList(BuildUtil.moveFileToPos(request.getSources(), mainSourceName, 0))));
      cmdBuilder.putVariable("inputLibraries", new ListVariable(BuildUtil.toStringList(request.getLibraries())));
      cmdBuilder.putVariable("inputLibs", new ListVariable(BuildUtil.toStringList(request.getLibraries())));
      cmdBuilder.putVariable("outputFile", new StringVariable(outFile.getAbsolutePath()));
      cmdBuilder.putVariable("version", new StringVariable(request.getVersion().toString()));
   }

   @Override
   protected String getEnvPath() {
      return System.getenv("PATH");
   }
}
