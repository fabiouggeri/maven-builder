/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools;

import org.uggeri.build.tools.log.Log;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author fabio_uggeri
 */
public interface ExecutionRequest {

   Log getLog();

   void setLog(Log log);

   String getOutputDir();

   void setOutputDir(String outputDir);

   String getOutputFileExtension();

   void setOutputFileExtension(String extension);

   Properties getProperties();

   void setProperties(Properties properties);

   String getOption(String prop);

   String getOption(String prop, String defaultValue);

   boolean containsOption(String prop);
   
   Map<String, String> getEnvironmentVariables();
   
   void setEnvironmentVariables(Map<String, String> environmentVariables);
   
   String getPathSeparator();
}
