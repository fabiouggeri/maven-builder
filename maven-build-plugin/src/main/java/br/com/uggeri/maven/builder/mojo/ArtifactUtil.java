/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 *
 * @author fabio_uggeri
 */
public class ArtifactUtil {

   public final static String artifactName(Artifact artifact) {
      StringBuilder sb = new StringBuilder();
      sb.append(artifact.getArtifactId()).append('-');
      if (artifact.getVersion().isEmpty()) {
         sb.append(artifact.getBaseVersion());
      } else {
         sb.append(artifact.getVersion());
      }
      if (artifact.hasClassifier()) {
         sb.append('-').append(artifact.getClassifier());
      }
      sb.append('.').append(artifact.getType());
      return sb.toString();
   }

   public final static String artifactPomName(Artifact artifact) {
      StringBuilder sb = new StringBuilder();
      sb.append(artifact.getArtifactId()).append('-');
      if (artifact.getVersion().isEmpty()) {
         sb.append(artifact.getBaseVersion());
      } else {
         sb.append(artifact.getVersion());
      }
      if (artifact.hasClassifier()) {
         sb.append('-').append(artifact.getClassifier());
      }
      sb.append(".pom");
      return sb.toString();
   }

   static boolean areEqual(Artifact a, Dependency d) {
      return a.getGroupId().equals(d.getGroupId())
         && a.getArtifactId().equals(d.getArtifactId())
         && a.getVersion().equals(d.getVersion())
         && a.getType().equals(d.getType())
         && ((a.getClassifier() == null && d.getClassifier() == null) || a.getClassifier().equals(d.getClassifier()));
   }
}
