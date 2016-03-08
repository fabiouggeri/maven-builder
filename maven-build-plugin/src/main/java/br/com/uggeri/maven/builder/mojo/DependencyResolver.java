/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

/**
 *
 * @author fabio_uggeri
 */
public class DependencyResolver {

   private final ArtifactRepository localRepository;

   private final RepositorySystem repositorySystem;

   private final Log log;

   public DependencyResolver(RepositorySystem repositorySystem, ArtifactRepository localRepository, Log log) {
      this.repositorySystem = repositorySystem;
      this.localRepository = localRepository;
      this.log = log;
   }

   public DependencyResolver(RepositorySystem repositorySystem, ArtifactRepository localRepository) {
      this(repositorySystem, localRepository, null);
   }

   public Artifact resolveDependency(final MavenProject project, final Dependency dependency) throws MojoExecutionException {
      final Artifact artifact = createArtifact(dependency);
      resolveArtifact(project, artifact);
      return artifact;
   }

   private void resolveArtifact(final MavenProject project, final Artifact artifact) {
      repositorySystem.resolve(createArtifactResolutionRequest(artifact, localRepository, project.getRemoteArtifactRepositories()));
   }

   private ArtifactResolutionRequest createArtifactResolutionRequest(Artifact artifact, ArtifactRepository localRepo, List<ArtifactRepository> remoteRepos) {
      ArtifactResolutionRequest resolutionRequest = new ArtifactResolutionRequest();
      resolutionRequest.setArtifact(artifact);
      resolutionRequest.setLocalRepository(localRepo);
      resolutionRequest.setRemoteRepositories(remoteRepos);
      return resolutionRequest;
   }

   private Artifact createArtifact(final Dependency dependency) {
      return repositorySystem.createArtifactWithClassifier(dependency.getGroupId(),
         dependency.getArtifactId(),
         dependency.getVersion(),
         dependency.getType(),
         dependency.getClassifier());
   }

   private Artifact createArtifact(final String groupId, final String artifactId, final String version, final String type) {
      return repositorySystem.createArtifact(groupId, artifactId, version, type);
   }

   public List<Artifact> resolveDependencies(MavenProject project, Artifact artifact) throws MojoExecutionException {
      final List<Artifact> artifactDependencies = new ArrayList<Artifact>();
      Artifact pomArtifact = createArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "pom");
      resolveArtifact(project, pomArtifact);
      if (pomArtifact.isResolved()) {
         DefaultModelReader dmr = new DefaultModelReader();
         try {
            Model pomModel = dmr.read(pomArtifact.getFile(), null);
            for (Dependency dep : pomModel.getDependencies()) {
               final Artifact depArtifact = createArtifact(dep);
               if (log != null && log.isDebugEnabled()) {
                  log.debug("Dependencia encontrada para " + artifact.getId() + ".");
               }
               resolveArtifact(project, depArtifact);
               if (depArtifact.isResolved()) {
                  artifactDependencies.add(depArtifact);
               }
            }
         } catch (IOException ex) {
            throw new MojoExecutionException("Erro ao ler o arquivo POM do artefato " + artifact.getId(), ex);
         }
      }
      return artifactDependencies;
   }
}
