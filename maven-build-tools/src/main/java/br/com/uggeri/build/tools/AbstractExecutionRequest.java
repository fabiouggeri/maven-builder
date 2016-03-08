/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import br.com.uggeri.build.tools.log.Log;
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

}
