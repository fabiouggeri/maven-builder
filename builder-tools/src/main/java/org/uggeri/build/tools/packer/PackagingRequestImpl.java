/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.packer;

import org.uggeri.build.tools.Version;
import org.uggeri.build.tools.AbstractExecutionRequest;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ADMIN
 */
public class PackagingRequestImpl extends AbstractExecutionRequest implements PackagingRequest {

   private List<File> sources = null;

   private List<File> libraries = null;

   private String mainSourceFileName = null;

   private String outputFileName = null;

   private Version version = null;

   private Map<String, String> environmentVariables = null;
   
   @Override
   public List<File> getSources() {
      if (sources == null) {
         return Collections.emptyList();
      }
      return sources;
   }

   @Override
   public void setSources(List<File> sources) {
      this.sources = sources;
   }

   /**
    * @return the libraries
    */
   @Override
   public List<File> getLibraries() {
      if (libraries == null) {
         return Collections.emptyList();
      }
      return libraries;
   }

   @Override
   public void setLibraries(List<File> libraries) {
      this.libraries = libraries;
   }

   @Override
   public void setMainSourceFileName(String mainSource) {
      mainSourceFileName = mainSource;
   }

   /**
    * @return the mainSourceFileName
    */
   @Override
   public String getMainSourceFileName() {
      return mainSourceFileName;
   }

   /**
    * @return the outputFileName
    */
   @Override
   public String getOutputFileName() {
      return outputFileName;
   }

   /**
    * @param outputFileName the outputFileName to set
    */
   @Override
   public void setOutputFileName(String outputFileName) {
      this.outputFileName = outputFileName;
   }

   @Override
   public Version getVersion() {
      return version;
   }

   @Override
   public void setVersion(Version version) {
      this.version = version;
   }

   @Override
   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }

   @Override
   public void setEnvironmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
   }
}
