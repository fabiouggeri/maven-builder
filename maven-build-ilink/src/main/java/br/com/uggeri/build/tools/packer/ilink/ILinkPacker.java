/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer.ilink;

import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.packer.AbstractExternalPacker;
import br.com.uggeri.build.tools.packer.PackagingRequest;
import java.io.File;
import java.util.*;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public class ILinkPacker extends AbstractExternalPacker {

   public static final String DEFAULT_MAIN_SOURCE_NAME = "main";

   private static final String RESPONSE_FILENAME = "_ilink32.rsp";

   private static final List<String> supportedPackagings = Arrays.asList(new String[]{"exe"});

   private static final List<String> supportedTypes = Arrays.asList(new String[]{"obj"});

   public ILinkPacker() {
      super();
   }

   public ILinkPacker(File executable) {
      super(executable);
   }

   @Override
   protected Commandline getCommandLine(PackagingRequest request, File outFile) {
      StringBuilder commandStr = new StringBuilder(10 + request.getSources().size() * 8);
      StringBuilder objFileNames = new StringBuilder(request.getSources().size() * 8);
      Commandline commandLine = null;
      final String mainSourceName;
      List<File> libs = new ArrayList<File>();
      File rspFile;

      if (request.getMainSourceFileName() != null) {
         mainSourceName = BuildUtil.removePath(BuildUtil.removePath(BuildUtil.removeExtension(request.getMainSourceFileName()),'/'),'\\');
      } else {
         mainSourceName = DEFAULT_MAIN_SOURCE_NAME;
      }
      /* Se a dependencia de execucao eh com uma lib, remove e coloca
         * nas libs a serem utilizadas para linkar o executavel.
         * OBS.: O ILINK32 parece ter uma dependencia com a biblioteca
         * UUID.LIB e so funciona se ela estiver exatamente com este nome. */
      for (Iterator<File> it = getExecutionDependencies().iterator(); it.hasNext();) {
         File depFile = it.next();
         if (BuildUtil.fileExtension(depFile).toLowerCase().equals("lib")) {
            it.remove();
            libs.add(depFile);
         }
      }
      if (libs.size() > 0) {
         commandStr.append(" -L").append(BuildUtil.encloseFilePathName(BuildUtil.filesPath(libs), false));
      }
      if (getToolConfig() != null && getToolConfig().getStartOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getStartOptions());
      } else {
         commandStr.append(" -ap -Tpe");
      }
      if (request.getOption("debug").equalsIgnoreCase("true")) {
         commandStr.append(" -v");
      }
      if (request.getOption("verbose").equalsIgnoreCase("full")) {
         commandStr.append(" -r");
      }

      if (getToolConfig() != null && getToolConfig().getEndOptions() != null) {
         commandStr.append(' ').append(getToolConfig().getEndOptions());
      }
      for(File obj : request.getSources()) {
         final String objName = BuildUtil.nameWithoutExtension(obj);
         // Se o objeto principal joga para o inicio da lista
         if (mainSourceName.equals(objName)) {
            objFileNames.insert(0, " " + BuildUtil.encloseFilePathName(BuildUtil.canonicalPathName(obj), false));
         } else {
            objFileNames.append(' ').append(BuildUtil.encloseFilePathName(BuildUtil.canonicalPathName(obj), false));
         }
      }
      commandStr.append(objFileNames);
      commandStr.append(",").append(BuildUtil.encloseFilePathName(outFile.getAbsolutePath(), false));
      if (request.getLibraries().size() > 0) {
         commandStr.append(",,");
         appendLibraries(commandStr, request);
      }
      rspFile = createReplaceFile(new File(request.getOutputDir()), RESPONSE_FILENAME, commandStr);

      /* Verifica se conseguiu gerar o comando */
      if (rspFile != null) {
         // Adiciona o pathName do executavel
         commandStr.setLength(0);
         appendExecutablePathName(commandStr, "ilink32.exe");
         commandStr.append(" @").append(BuildUtil.encloseFilePathName(rspFile.getName(), false));
         commandLine = BuildUtil.createCommandLine(commandStr.toString());
         commandLine.setWorkingDirectory(FileUtils.getPath(rspFile.getAbsolutePath()));
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
      for(File lib : request.getLibraries()) {
         commandStr.append(' ').append(BuildUtil.encloseFilePathName(BuildUtil.canonicalPathName(lib), false));
      }
   }

   @Override
   public Collection<String> getSupportedPackagings() {
      return supportedPackagings;
   }
}
