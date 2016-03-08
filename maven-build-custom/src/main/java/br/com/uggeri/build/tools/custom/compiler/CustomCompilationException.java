/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.compiler;

/**
 *
 * @author Fabio
 */
public class CustomCompilationException extends RuntimeException {

   public CustomCompilationException(String msg) {
      super(msg);
   }  

   public CustomCompilationException(String msg, Throwable ex) {
      super(msg, ex);
   }
}
