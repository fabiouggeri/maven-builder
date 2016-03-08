/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.tools.packer.mslink;

import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.packer.AbstractExternalPacker;
import br.com.uggeri.build.tools.packer.PackagingRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public class MSLinkPacker extends AbstractExternalPacker {

   public static final String DEFAULT_MAIN_SOURCE_NAME = "main";

   private static final List<String> supportedPackagings = Arrays.asList(new String[]{"exe"});

   private static final List<String> supportedTypes = Arrays.asList(new String[]{"obj"});

   private static final List<String> supportedLibraries = Arrays.asList(new String[]{"lib"});

   public MSLinkPacker() {
      super();
   }

   public MSLinkPacker(File executable) {
      super(executable);
   }

   @Override
   protected Commandline getCommandLine(PackagingRequest request, File outFile) {
      StringBuilder commandStr = new StringBuilder(10 + request.getSources().size() * 8);
      StringBuilder objFileNames = new StringBuilder(request.getSources().size() * 8);
      Commandline commandLine = null;
      final String mainSourceName;
      File commandFile;

      if (request.getMainSourceFileName() != null) {
         mainSourceName = BuildUtil.removePath(BuildUtil.removePath(BuildUtil.removeExtension(request.getMainSourceFileName()), '/'), '\\');
      } else {
         mainSourceName = DEFAULT_MAIN_SOURCE_NAME;
      }

      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getStartOptions());
      } else {
         commandStr.append(" /INCREMENTAL:no /NODEFAULTLIB /SUBSYSTEM:CONSOLE /FORCE:MULTIPLE ");
      }

      if (request.getVersion() != null) {
         commandStr.append(" /VERSION:").append(request.getVersion().getMajor()).append('.').append(request.getVersion().getMinor());
      }

      if (request.getOption("debug").equalsIgnoreCase("true")) {
         commandStr.append(" /DEBUG");
      }

      if (request.getOption("verbose").equalsIgnoreCase("full")) {
         commandStr.append(" /VERBOSE");
      } else {
         commandStr.append(" /IGNORE:4006");
      }

      commandStr.append(" /OUT:").append(BuildUtil.encloseFilePathName(outFile.getAbsolutePath(), false));

      if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getEndOptions());
      }

      for (File obj : request.getSources()) {
         final String objName = BuildUtil.nameWithoutExtension(obj);
         // Se o objeto principal joga para o inicio da lista
         if (mainSourceName.equals(objName)) {
            objFileNames.insert(0, " " + BuildUtil.encloseFilePathName(BuildUtil.canonicalPathName(obj), false));
         } else {
            objFileNames.append(' ').append(BuildUtil.encloseFilePathName(BuildUtil.canonicalPathName(obj), false));
         }
      }
      commandStr.append(objFileNames);

      appendLibraries(commandStr, request);

      commandFile = createReplaceFile(new File(request.getOutputDir()), "_mslnkcmd.txt", commandStr);

      if (commandFile != null) {
         // Adiciona o pathName do executavel
         commandStr.setLength(0);
         appendExecutablePathName(commandStr, "link.exe");
         commandStr.append(" @").append(BuildUtil.encloseFilePathName(commandFile.getName(), false));
         commandLine = BuildUtil.createCommandLine(commandStr.toString());
         commandLine.setWorkingDirectory(FileUtils.getPath(commandFile.getAbsolutePath()));
      }
      return commandLine;
   }

   @Override
   public Collection<String> supportedTypes() {
      return supportedTypes;
   }

   @Override
   public String getDefaultOutputExtension() {
      return supportedPackagings.get(0);
   }

   private void appendLibraries(StringBuilder commandStr, PackagingRequest request) {
      for (File lib : request.getLibraries()) {
         commandStr.append(" ").append(BuildUtil.encloseFilePathName(BuildUtil.canonicalPathName(lib)));
      }
   }

   @Override
   public Collection<String> getSupportedPackagings() {
      return supportedPackagings;
   }
}
