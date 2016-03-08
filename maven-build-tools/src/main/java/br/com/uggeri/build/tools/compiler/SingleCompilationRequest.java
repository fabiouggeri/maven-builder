/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import java.io.File;

/**
 *
 * @author fabio_uggeri
 */
public class SingleCompilationRequest extends AbstractCompilationRequest {

   private final File source;

   public SingleCompilationRequest(File source) {
      this.source = source;
   }

   @Override
   public boolean isBatch() {
      return false;
   }

   public File getSource() {
      return source;
   }
}
