/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools;

import java.io.File;

/**
 *
 * @author fabio_uggeri
 */
public class OptionsFile {

   private File pathName;
   
   private String content;

   /**
    * @return the pathName
    */
   public File getPathName() {
      return pathName;
   }

   /**
    * @param pathName the pathName to set
    */
   public void setPathName(File pathName) {
      this.pathName = pathName;
   }

   /**
    * @return the content
    */
   public String getContent() {
      return content;
   }

   /**
    * @param content the content to set
    */
   public void setContent(String content) {
      this.content = content;
   }
}