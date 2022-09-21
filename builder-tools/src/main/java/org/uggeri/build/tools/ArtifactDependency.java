/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools;

/**
 *
 * @author fabio_uggeri
 */
public class ArtifactDependency {

   private String groupId;
   private String artifactId;
   private String version;
   private String type;
   private String classifier;
   private String scope;

   public ArtifactDependency(String groupId, String artifactId, String version, String type, String classifier, String scope) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.type = type;
      this.scope = scope;
      this.classifier = classifier;
   }

   public ArtifactDependency() {
      this(null, null, null, null, null, null);
   }

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
    * @return the artifactId
    */
   public String getArtifactId() {
      return artifactId;
   }

   /**
    * @param artifactId the artifactId to set
    */
   public void setArtifactId(String artifactId) {
      this.artifactId = artifactId;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getScope() {
      if (scope == null) {
         this.scope = "compile";
      }
      return scope;
   }

   public void setScope(String scope) {
      this.scope = scope;
   }

   /**
    * @return the type
    */
   public String getType() {
      if (type == null) {
         return "jar";
      }
      return type;
   }

   /**
    * @param type the type to set
    */
   public void setType(String type) {
      this.type = type;
   }

   /**
    * @return the classifier
    */
   public String getClassifier() {
      return classifier;
   }

   /**
    * @param classifier the classifier to set
    */
   public void setClassifier(String classifier) {
      this.classifier = classifier;
   }

   @Override
   public String toString() {
      String result;
      if (getGroupId() != null) {
         result = getGroupId();
      } else  {
         result = "";
      }
      if (getArtifactId() != null) {
         result += ":" + getArtifactId();
      }
      if (getType() != null) {
         result += ":" + getType();
      }
      if (getClassifier() != null) {
         result += ":" + getClassifier();
      }
      return result;
   }
}
