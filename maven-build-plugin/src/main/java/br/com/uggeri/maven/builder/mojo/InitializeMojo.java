/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Initialize build lifecycle
 */
@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeMojo extends AbstractInitializeMojo {

   @Parameter
   private Map<String, String> envVars;

   @Override
   public void execute() throws MojoExecutionException {
      initialize();
      initializeEnvironmentVars();
   }

   private void initializeEnvironmentVars() {
      if (getPreparedEnvVars() != null) {
         if (envVars == null) {
            envVars = new HashMap<String, String>();
         }
         for (Map.Entry<String, String> envVar : getPreparedEnvVars().entrySet()) {
            if (!envVars.containsKey(envVar.getKey())) {
               envVars.put(envVar.getKey(), envVar.getValue());
            }
         }
      }
      getBuildContext().setEnvironmentVariables(envVars);
   }

   public Map<String, String> getEnvVars() {
      return envVars;
   }

   public void setEnvVars(Map<String, String> envVars) {
      this.envVars = envVars;
   }
}
