/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools;

import java.io.File;
import org.uggeri.build.tools.log.Log;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author ADMIN
 */
public abstract class AbstractExecutionRequest implements ExecutionRequest {

   private String outputDir = null;

   private String outputFileExtension = null;

   private Properties properties = null;

   private Log log = null;

   private Map<String, String> environmentVariables = null;
   
   private String pathSeparator = File.separator;

   /**
    * @return the outputDir
    */
   @Override
   public String getOutputDir() {
      return outputDir;
   }

   /**
    * @param outputDir the outputDir to set
    */
   @Override
   public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
   }

   /**
    * @return the outputFileExtension
    */
   @Override
   public String getOutputFileExtension() {
      return outputFileExtension;
   }

   /**
    * @param outputFileExtension the outputFileExtension to set
    */
   @Override
   public void setOutputFileExtension(String outputFileExtension) {
      this.outputFileExtension = outputFileExtension;
   }

   @Override
   public String getOption(String prop) {
      if (properties != null) {
         return properties.getProperty(prop, "false");
      }
      return "false";
   }

   @Override
   public String getOption(String prop, String defaultValue) {
      if (properties != null) {
         return properties.getProperty(prop, defaultValue);
      }
      return defaultValue;
   }

   @Override
   public boolean containsOption(String prop) {
      if (properties != null) {
         return properties.containsKey(prop);
      }
      return false;
   }

   @Override
   public Log getLog() {
      return log;
   }

   @Override
   public void setLog(Log log) {
      this.log = log;
   }

   @Override
   public Properties getProperties() {
      return properties;
   }

   @Override
   public void setProperties(Properties properties) {
      this.properties = properties;
   }

   @Override
   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }

   @Override
   public void setEnvironmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
   }

   @Override
   public String getPathSeparator() {
      return pathSeparator;
   }

   public void setPathSeparator(String pathSeparator) {
      this.pathSeparator = pathSeparator;
   }
}
