/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.maven.builder.mojo;

import org.uggeri.build.tools.ArtifactDependency;
import org.uggeri.build.tools.ArtifactToolDependency;
import org.uggeri.build.tools.ToolConfig;

/**
 *
 * @author fabio_uggeri
 */
public class ToolMapping {

   private ArtifactToolDependency tool;

   private ArtifactDependency executable;

   private ToolConfig toolConfig;

   /**
    * @return the tool
    */
   public ArtifactToolDependency getTool() {
      return tool;
   }

   /**
    * @param tool the tool to set
    */
   public void setTool(ArtifactToolDependency tool) {
      this.tool = tool;
   }

   /**
    * @return the executable
    */
   public ArtifactDependency getExecutable() {
      return executable;
   }

   /**
    * @param executable the executable to set
    */
   public void setExecutable(ArtifactDependency executable) {
      this.executable = executable;
   }

   public ToolConfig getToolConfig() {
      return toolConfig;
   }

   public void setToolConfig(ToolConfig toolConfig) {
      this.toolConfig = toolConfig;
   }
}
