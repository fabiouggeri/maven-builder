/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.compiler.project;

/**
 *
 * @author fabio_uggeri
 */
public class CompilationException extends Exception {

   public CompilationException(String errorMsg, Exception ex) {
      super(errorMsg, ex);
   }
}
