/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import br.com.uggeri.build.tools.log.Log;
import java.util.Properties;

/**
 *
 * @author fabio_uggeri
 */
public interface ExecutionRequest {

   public Log getLog();

   public void setLog(Log log);

   public String getOutputDir();

   public void setOutputDir(String outputDir);

   public String getOutputFileExtension();

   public void setOutputFileExtension(String extension);

   public Properties getProperties();

   public void setProperties(Properties properties);

   public String getOption(String prop);

   public String getOption(String prop, String defaultValue);

   public boolean containsOption(String prop);
}
