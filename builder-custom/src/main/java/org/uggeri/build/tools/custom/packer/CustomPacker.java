/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.custom.packer;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.custom.CustomCommandLineBuilder;
import org.uggeri.build.tools.custom.vars.ListVariable;
import org.uggeri.build.tools.custom.vars.StringVariable;
import org.uggeri.build.tools.packer.AbstractExternalPacker;
import org.uggeri.build.tools.packer.PackagingRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.uggeri.build.tools.OptionsFile;

/**
 *
 * @author Fabio
 */
public class CustomPacker extends AbstractExternalPacker {

   public CustomPacker() {
      super();
   }

   @Override
   protected Commandline getCommandLine(PackagingRequest request, File outFile) {
      final CustomCommandLineBuilder cmdBuilder;
      final Commandline commandLine;
      File baseDir;
      cmdBuilder = new CustomCommandLineBuilder();
      baseDir = BuildUtil.baseDirectory(request.getSources());
      if (baseDir == null) {
         baseDir = new File(".");
      }
      createVariables(cmdBuilder, request, outFile, baseDir);
      createOptionsFile(cmdBuilder, request, getToolConfig().getOptionsFile());
      commandLine = BuildUtil.createCommandLine(cmdBuilder.buildCommandLine(getToolConfig().getCommandLine(), this, request));
      commandLine.setWorkingDirectory(baseDir);
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

   private void createVariables(CustomCommandLineBuilder cmdBuilder, PackagingRequest request, File outFile, File baseDir) {
      final String mainSourceName;
      if (request.getMainSourceFileName() != null) {
         mainSourceName = BuildUtil.removePath(BuildUtil.removePath(BuildUtil.removeExtension(request.getMainSourceFileName()),'/'),'\\');
      } else {
         mainSourceName = "main";
      }
      if (getExecutable() != null) {
         cmdBuilder.putVariable("toolExe", new StringVariable(BuildUtil.normalizePath(getExecutable().getAbsolutePath(), request.getPathSeparator())));
      }
      cmdBuilder.putVariable("mainSourceFile", new StringVariable(BuildUtil.normalizePath(request.getMainSourceFileName(), request.getPathSeparator())));
      cmdBuilder.putVariable("inputFiles", new ListVariable(BuildUtil.normalizePath(BuildUtil.fileListToStringList(baseDir, BuildUtil.moveFileToPos(request.getSources(), mainSourceName, 0), true), request.getPathSeparator())));
      cmdBuilder.putVariable("inputLibraries", new ListVariable(BuildUtil.normalizePath(BuildUtil.fileListToStringList(baseDir, request.getLibraries(), true), request.getPathSeparator())));
      cmdBuilder.putVariable("librariesPaths", new ListVariable(BuildUtil.normalizePath(librariesPaths(request.getLibraries()), request.getPathSeparator())));
      cmdBuilder.putVariable("inputLibs", new ListVariable(BuildUtil.normalizePath(BuildUtil.fileListToStringList(baseDir, request.getLibraries(), true), request.getPathSeparator())));
      cmdBuilder.putVariable("outputFile", new StringVariable(BuildUtil.normalizePath(outFile.getAbsolutePath(), request.getPathSeparator())));
      cmdBuilder.putVariable("version", new StringVariable(request.getVersion().toString()));
   }

   @Override
   protected String getEnvPath() {
      return System.getenv("PATH");
   }

   private void createOptionsFile(CustomCommandLineBuilder cmdBuilder, PackagingRequest request, OptionsFile optionsFile) {
      if (optionsFile != null && optionsFile.getPathName() != null && ! StringUtils.isEmpty(optionsFile.getContent())) {
         try {
            final String fileContent = cmdBuilder.buildCommandLine(optionsFile.getContent(), this, request);
            FileUtils.fileWrite(optionsFile.getPathName(), fileContent);
         } catch (IOException ex) {
            request.getLog().error("Erro ao criar o arquivo de opcoes '" + optionsFile.getPathName() + "'.", ex);
         }
      }
   }

   private List<String> librariesPaths(List<File> libraries) {
      final Set<String> librariesPaths = new HashSet<>();
      final boolean caseSensitiveFileSystem = ! BuildUtil.isWindows();
      for (File lib : libraries) {
         if (caseSensitiveFileSystem) {
            librariesPaths.add(lib.getParentFile().getAbsolutePath());
         } else {
            librariesPaths.add(lib.getParentFile().getAbsolutePath().toLowerCase());
         }
      }
      return new ArrayList<>(librariesPaths);
   }
}
