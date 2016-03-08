/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author fabio_uggeri
 */
public abstract class AbstractExecutionResult implements ExecutionResult {

   private Collection<String> output;
   
   private int exitCode;

   public AbstractExecutionResult(Collection<String> output, int exitCode) {
      this.output = output;
      this.exitCode = exitCode;
   }

   public AbstractExecutionResult() {
      this(null, 0);
   }
   
   @Override
   public Collection<String> getOutput() {
      if (output == null) {
         return Collections.emptyList();
      }
      return output;
   }

   public void setOutput(Collection<String> output) {
      this.output = output;
   }

   @Override
   public int getExitCode() {
      return exitCode;
   }

   public void setExitCode(int exitCode) {
      this.exitCode = exitCode;
   }

   @Override
   public boolean isSuccessful() {
      return exitCode == 0;
   }
   
}
