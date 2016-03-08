/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer;

import br.com.uggeri.build.tools.AbstractExecutionResult;
import java.io.File;
import java.util.Collection;

/**
 *
 * @author fabio_uggeri
 */
public class PackagingResultImpl extends AbstractExecutionResult implements PackagingResult {

   private final File outputFile;

   public PackagingResultImpl(File outputFile, Collection<String> output, int exitCode) {
      super(output, exitCode);
      this.outputFile = outputFile;
   }
   
   public PackagingResultImpl(File outputFile) {
      this.outputFile = outputFile;
   }
   
   
   @Override
   public File getOutputFile() {
      return outputFile;
   }

   @Override
   public boolean isSuccessful() {
      return super.isSuccessful() && outputFile != null;
   }
   
   
}
