/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.packer.zip;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.ToolConfig;
import org.uggeri.build.tools.ToolType;
import org.uggeri.build.tools.log.Log;
import org.uggeri.build.tools.packer.PackagingRequest;
import org.uggeri.build.tools.packer.Packer;
import org.uggeri.build.tools.packer.PackagingResult;
import org.uggeri.build.tools.packer.PackagingResultImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author ADMIN
 */
public class ZipPacker implements Packer {

   private static final List<String> supportedPackagings = Arrays.asList(new String[]{"zip", "include", "plsql"});

   private static final List<String> supportedTypes = Arrays.asList(new String[]{"ch", "h", "hpp", "api", "fnc", "pck", "pkb", "pks", "prc"});

   @Override
   public PackagingResult execute(PackagingRequest request) {
      File outFile = outputFile(request);
      final List<String> executionOutput = new ArrayList<>();
      final Log log = request.getLog();
      int exitCode = 0;

      Iterator<File> iterator = request.getSources().iterator();
      if (iterator.hasNext()) {
         if (BuildUtil.zip(request.getSources(), outFile, iterator.next().getParentFile(), log, false) != null) {
            executionOutput.add("Error packaging " + outFile + ".");
            outFile = null;
            exitCode = 1;
         }
      } else {
         executionOutput.add("File not found for packaging.");
         outFile = null;
            exitCode = 1;
      }
      return new PackagingResultImpl(outFile, executionOutput, exitCode);
   }

   private File outputFile(PackagingRequest request) {
      final String trimFileName = request.getOutputFileName().trim();
      final String outFileName = BuildUtil.removeExtension(trimFileName);
      String fileExt = BuildUtil.fileExtension(trimFileName);

      // Verifica a extensao
      if (request.getOutputFileExtension() == null) {
         if (fileExt == null || fileExt.isEmpty()) {
            fileExt = getDefaultOutputExtension();
         }
      } else {
         fileExt = request.getOutputFileExtension().trim();
         if (fileExt.isEmpty()) {
            fileExt = getDefaultOutputExtension();
         }
      }
      return new File(BuildUtil.removeLastPathSeparator(request.getOutputDir()) + File.separatorChar + outFileName + '.' + fileExt);
   }

   @Override
   public String getDefaultOutputExtension() {
      return supportedPackagings.get(0);
   }

   @Override
   public Collection<String> getSupportedPackagings() {
      return supportedTypes;
   }

   @Override
   public Collection<String> supportedTypes() {
      return supportedTypes;
   }

   @Override
   public ToolType getToolType() {
      return ToolType.PACKER;
   }

   @Override
   public ToolConfig getToolConfig() {
      return null;
   }

   @Override
   public void setToolConfig(ToolConfig config) {
   }
}
