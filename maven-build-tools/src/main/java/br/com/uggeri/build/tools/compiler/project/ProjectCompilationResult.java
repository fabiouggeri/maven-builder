/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.tools.compiler.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public class ProjectCompilationResult {

   private final List<File> outputFiles = new ArrayList<File>();

   private List<String> errors = null;

   public synchronized void addError(final String error) {
      if (errors == null) {
         errors = new ArrayList<String>();
      }
      errors.add(error);
   }

   public List<String> getErrors() {
      if (errors == null) {
         return Collections.emptyList();
      }
      return errors;
   }


   /**
    * @return the outputFiles
    */
   public List<File> getOutputFiles() {
      return outputFiles;
   }
}
