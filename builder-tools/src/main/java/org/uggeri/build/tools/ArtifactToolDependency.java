/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools;

/**
 *
 * @author fabio_uggeri
 */
public class ArtifactToolDependency extends ArtifactDependency {

   private String toolClass;

   public String getToolClass() {
      return toolClass;
   }

   public void setToolClass(String toolClass) {
      this.toolClass = toolClass;
   }
}
