/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.maven.builder.mojo;

/**
 *
 * @author fabio_uggeri
 */
public class Classifier {
 
   private String groupId = null;

   private String classifier = null;

   /**
    * @return the groupId
    */
   public String getGroupId() {
      return groupId;
   }

   /**
    * @param groupId the groupId to set
    */
   public void setGroupId(String groupId) {
      this.groupId = groupId;
   }

   /**
    * @return the classifier value
    */
   public String getClassifier() {
      return classifier;
   }

   /**
    * @param set classifier value
    */
   public void setClassifier(String classifier) {
      this.classifier = classifier;
   }
}
