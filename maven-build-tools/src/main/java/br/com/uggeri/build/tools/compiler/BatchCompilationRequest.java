/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import java.io.File;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public class BatchCompilationRequest extends AbstractCompilationRequest {

   private List<File> sources;

   public BatchCompilationRequest(List<File> sources) {
      this.sources = sources;
   }
   
   @Override
   public boolean isBatch() {
      return true;
   }

   public List<File> getSources() {
      return sources;
   }
   
}
