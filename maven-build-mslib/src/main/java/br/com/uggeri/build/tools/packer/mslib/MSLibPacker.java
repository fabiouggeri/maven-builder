/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer.mslib;

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
public class MSLibPacker extends AbstractExternalPacker {

   private static final List<String> supportedPackagings = Arrays.asList(new String[]{"lib"});

   private static final List<String> supportedTypes = Arrays.asList(new String[]{"obj"});

   public MSLibPacker() {
      super();
   }

   public MSLibPacker(File executable) {
      super(executable);
   }

   @Override
   protected Commandline getCommandLine(PackagingRequest request, File outFile) {
      StringBuilder commandStr = new StringBuilder();
      Commandline commandLine;

      // Adiciona o pathName do executavel
      appendExecutablePathName(commandStr, "lib.exe");

      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getStartOptions());
      } else {
         commandStr.append(" /SUBSYSTEM:CONSOLE");
      }

      if (request.getOption("verbose").equalsIgnoreCase("full")) {
         commandStr.append(" /VERBOSE");
      }

      // Adiciona o pathName do arquivo de saida
      commandStr.append(" /OUT:").append(BuildUtil.encloseFilePathName(outFile.getName()));

      for(File obj : request.getSources()) {
         // Adiciona o pathName do codigo fonte
         commandStr.append(' ').append(BuildUtil.encloseFilePathName(BuildUtil.getRelativePath(outFile.getParentFile(), obj)));
      }

      if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getEndOptions());
      }

      commandLine = BuildUtil.createCommandLine(commandStr.toString());
      commandLine.setWorkingDirectory(FileUtils.getPath(outFile.getAbsolutePath()));
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

   @Override
   public Collection<String> getSupportedPackagings() {
      return supportedPackagings;
   }
}
