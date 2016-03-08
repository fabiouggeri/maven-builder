/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.tools;

import br.com.uggeri.build.tools.log.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 * @param <T>
 * @param <R>
 */
public abstract class AbstractExternalTool<T extends ExecutionResult, R extends ExecutionRequest> implements ExternalTool<T, R> {

   private File executable;

   private Collection<File> executionDependencies;

   private ToolConfig toolConfig;

   public AbstractExternalTool() {
   }

   public AbstractExternalTool(File executable) {
      this.executable = executable;
   }

   /**
    * @return the executable
    */
   @Override
   public File getExecutable() {
      return executable;
   }

   /**
    * @param executable the executable to set
    */
   @Override
   public void setExecutable(File executable) {
      this.executable = executable;
   }

   @Override
   public void setExecutionDependencies(Collection<File> executionDependencies) {
      this.executionDependencies = executionDependencies;
   }

   @Override
   public Collection<File> getExecutionDependencies() {
      return executionDependencies;
   }

   protected File createReplaceFile(File dir, String fileName, CharSequence content) {
      FileWriter fw = null;
      File file = null;
      try {
         file = new File(dir, fileName);
         fw = new FileWriter(file);
         fw.write(content.toString());
      } catch (IOException ex) {
         file = null;
      } finally {
         if (fw != null) {
            try {
               fw.close();
            } catch (IOException ex) {
            }
         }
      }
      return file;
   }

   @Override
   public ToolConfig getToolConfig() {
      return toolConfig;
   }

   @Override
   public void setToolConfig(ToolConfig config) {
      this.toolConfig = config;
   }

   protected String getEnvPath() {
      return "";
   }

   protected StringBuilder appendPathSep(StringBuilder path) {
      if (path.length() > 0) {
         path.append(File.pathSeparator);
      }
      return path;
   }

   protected void appendExecutablePathName(StringBuilder commandStr, final String defaultExecutableName) {
      if (getExecutable().isDirectory()) {
         commandStr.append(getExecutable().getAbsolutePath()).append(File.separatorChar).append(defaultExecutableName);
      } else {
         commandStr.append(BuildUtil.encloseFilePathName(getExecutable().getAbsolutePath(), false));
      }
   }

   protected void debugEnvironmentVariables(final Log log, final Commandline command) {
      if (log.isDebugEnabled()) {
         try {
            for (String varSet : command.getEnvironmentVariables()) {
               log.debug("SET " + varSet);
            }
         } catch (CommandLineException ex) {
            log.error("Error getting environment variables.");
         }
      }
   }

   protected void addConfiguredEnvironmentVariables(Commandline command, Map<String, String> envVars) {
      if (envVars != null) {
         for (Map.Entry<String, String> var : envVars.entrySet()) {
            command.addEnvironment(var.getKey(), var.getValue());
         }
      }
   }
}
