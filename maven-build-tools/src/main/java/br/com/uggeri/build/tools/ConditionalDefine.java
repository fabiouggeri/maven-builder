/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author fabio_uggeri
 */
public class ConditionalDefine {

   private String groupId = null;

   private String artifactId = null;

   private String version = null;

   private String type = null;

   private String scope = null;

   private Map<String, String> properties = null;

   private String define = null;

   private String value = null;

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

   /**
    * @return the version
    */
   public String getVersion() {
      return version;
   }

   /**
    * @param version the version to set
    */
   public void setVersion(String version) {
      this.version = version;
   }

   /**
    * @return the type
    */
   public String getType() {
      return type;
   }

   /**
    * @param type the type to set
    */
   public void setType(String type) {
      this.type = type;
   }

   /**
    * @return the scope
    */
   public String getScope() {
      return scope;
   }

   /**
    * @param scope the scope to set
    */
   public void setScope(String scope) {
      this.scope = scope;
   }

   /**
    * @return the define
    */
   public String getDefine() {
      return define;
   }

   /**
    * @param define the define to set
    */
   public void setDefine(String define) {
      this.define = define;
   }

   /**
    * @return the value
    */
   public String getValue() {
      return value;
   }

   /**
    * @param value the value to set
    */
   public void setValue(String value) {
      this.value = value;
   }

   /**
    * @return the properties
    */
   public Map<String, String> getProperties() {
      return properties;
   }

   /**
    * @param properties the properties to set
    */
   public void setProperties(Map<String, String> properties) {
      this.properties = properties;
   }
   
   public boolean isConditionSatisfied(Set<ArtifactDependency> dependencies, Properties projectProperties) {
      boolean satisfied = verifyConditionalDefineDependencies(dependencies);
      if (satisfied) {
         satisfied = verifyConditionalDefineProperties(projectProperties);
      }
      return satisfied;
   }
   
   private boolean startsWithGroupId(String defineGroupId, String artifactGroupId) {
      if (defineGroupId.length() > 0) {
         if (defineGroupId.charAt(defineGroupId.length() - 1) == '.') {
            return artifactGroupId.startsWith(defineGroupId);
         } else {
            return artifactGroupId.startsWith(defineGroupId + '.');
         }
      }
      return false;
   }

   private boolean verifyConditionalDefineDependencies(Set<ArtifactDependency> dependencies) {
      boolean satisfied = false;
      if (getGroupId() != null) {
         for (ArtifactDependency artifact : dependencies) {
            if (getGroupId().equals(artifact.getGroupId()) || startsWithGroupId(getGroupId(), artifact.getGroupId())) {
               if (getArtifactId() == null || getArtifactId().equals(artifact.getArtifactId())) {
                  if (getVersion() == null || getVersion().equals(artifact.getVersion())) {
                     if (getType() == null || getType().equals(artifact.getType())) {
                        if (getScope() == null || getScope().equals(artifact.getScope())) {
                           satisfied = true;
                           break;
                        }
                     }
                  }
               }
            }
         }
      } else {
         satisfied = true;
      }
      return satisfied;
   }

   private boolean verifyConditionalDefineProperties(Properties projectProperties) {
      boolean satisfied = false;
      if (getProperties() != null) {
         for (Entry property : getProperties().entrySet()) {
            if (!projectProperties.contains(property.getKey())) {
               satisfied = false;
               break;
            }
            if (property.getValue() != null && !property.getValue().equals(projectProperties.get(property.getKey()))) {
               satisfied = false;
               break;
            }
         }
      } else {
         satisfied = true;
      }
      return satisfied;
   }
}
