/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer.tlib;

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
public class TLibPacker extends AbstractExternalPacker {

   private static final List<String> supportedPackagings = Arrays.asList(new String[]{"lib"});

   private static final List<String> supportedTypes = Arrays.asList(new String[]{"obj"});

   public TLibPacker() {
      super();
   }

   public TLibPacker(File executable) {
      super(executable);
   }

   @Override
   protected Commandline getCommandLine(PackagingRequest request, File outFile) {
      StringBuilder commandStr = new StringBuilder();
      Commandline commandLine;

      // Adiciona o pathName do executavel
      appendExecutablePathName(commandStr, "tlib.exe");

      // Adiciona o pathName do arquivo de saida
      commandStr.append(' ').append(BuildUtil.encloseFilePathName(outFile.getName()));

      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getStartOptions());
      }

      for(File obj : request.getSources()) {
         // Adiciona o pathName do codigo fonte
         commandStr.append(" +").append(BuildUtil.encloseFilePathName(BuildUtil.getRelativePath(outFile.getParentFile(), obj)));
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
