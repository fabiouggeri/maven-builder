/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.uggeri.build.tools.BuildUtil;

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
         if (envVars != null) {
            for (Map.Entry<String, String> envVar : envVars.entrySet()) {
               final String values[];
               if (envVar.getValue() != null && envVar.getValue().indexOf(';') >= 0) {
                  values = envVar.getValue().split(";");
               } else {
                  values = new String[] { envVar.getValue() };
               }
               if (values.length > 1) {
                  envVar.setValue(BuildUtil.platformPathList(values));
               } else if (values.length > 0) {
                  envVar.setValue(values[0]);
               }
            }
         } else {
            envVars = new HashMap<>();
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
