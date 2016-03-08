/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import br.com.uggeri.build.tools.AbstractExecutionRequest;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ADMIN
 */
public abstract class AbstractCompilationRequest extends AbstractExecutionRequest implements CompilationRequest {

   private Map<String, String> defines = null;

   private List<String> includesPaths = null;

   private File sourceDirectory = null;

   private boolean force = false;

   private Map<String, String> environmentVariables = null;

   /**
    * @return the defines
    */
   @Override
   public Map<String, String> getDefines() {
      if (defines == null) {
         return Collections.emptyMap();
      }
      return defines;
   }

   @Override
   public void setDefines(Map<String, String> defines) {
      this.defines = defines;
   }

   /**
    * @return the includes
    */
   @Override
   public List<String> getIncludesPaths() {
      if (includesPaths == null) {
         return Collections.emptyList();
      }
      return includesPaths;
   }

   @Override
   public void setIncludesPaths(List<String> includesPaths) {
      this.includesPaths = includesPaths;
   }

   /**
    * @return the sourceDirectory
    */
   @Override
   public File getSourceDirectory() {
      return sourceDirectory;
   }

   /**
    * @param sourceDirectory the sourceDirectory to set
    */
   @Override
   public void setSourceDirectory(File sourceDirectory) {
      this.sourceDirectory = sourceDirectory;
   }

   public void setForce(boolean force) {
      this.force = force;
   }

   @Override
   public boolean isForce() {
      return force;
   }

   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }

   public void setEnvironmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
   }
}
