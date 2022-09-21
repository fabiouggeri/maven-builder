/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.helper.mojo;

import java.io.File;

/**
 *
 * @author fabio_uggeri
 */
public class ZipSource {
   
   private File sourcePathName;
   private String pathInZip;
   private String filterExtensions;
   private boolean recursive;

   /**
    * @return the pathInZip
    */
   public String getPathInZip() {
      return pathInZip;
   }

   /**
    * @param pathInZip the pathInZip to set
    */
   public void setPathInZip(String pathInZip) {
      this.pathInZip = pathInZip;
   }

   /**
    * @return the sourcePathName
    */
   public File getSourcePathName() {
      return sourcePathName;
   }

   /**
    * @param sourcePathName the sourcePathName to set
    */
   public void setSourcePathName(File sourcePathName) {
      this.sourcePathName = sourcePathName;
   }

   /**
    * @return the filterExtensions
    */
   public String getFilterExtensions() {
      return filterExtensions;
   }

   /**
    * @param filterExtensions the filterExtensions to set
    */
   public void setFilterExtensions(String filterExtensions) {
      this.filterExtensions = filterExtensions;
   }

   /**
    * @return the recursive
    */
   public boolean isRecursive() {
      return recursive;
   }

   /**
    * @param recursive the recursive to set
    */
   public void setRecursive(boolean recursive) {
      this.recursive = recursive;
   }

   @Override
   public String toString() {
      return (filterExtensions != null && ! filterExtensions.isEmpty() ? filterExtensions + " from " : "" ) + 
              sourcePathName + " to zip " + 
              pathInZip;
   }
   
   
}
