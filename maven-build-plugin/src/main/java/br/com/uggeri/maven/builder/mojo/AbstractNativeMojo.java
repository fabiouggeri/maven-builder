/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo;

import br.com.uggeri.build.tools.BuildUtil;
import br.com.uggeri.build.tools.ExecutionResult;
import br.com.uggeri.maven.builder.MavenLogWrapper;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.repository.RepositorySystem;

/**
 *
 * @author ADMIN
 */
public abstract class AbstractNativeMojo extends AbstractMojo implements Executor {

   private static final String BUILD_CONTEXT = "_BUILD_CONTEXT_";
   
   private final static Pattern WARNING_PATTERN = Pattern.compile("[^a-zA-Z]warn[^a-zA-Z]|[^a-zA-Z]warning[^a-zA-Z]", Pattern.CASE_INSENSITIVE);

   private final static Pattern ERROR_PATTERN = Pattern.compile("[^a-zA-Z]error([^a-zA-Z\\.]|(\\s*:))", Pattern.CASE_INSENSITIVE);

   /**
    * Mapear os executaveis para as ferramentas
    */
   @Parameter
   private List<ToolMapping> toolsMapping = null;

   /**
    * Arquivos que nao deverao ser compilados ou empacotados.
    */
   @Parameter
   private List<String> excludeSources = null;

   /**
    * Arquivos a serem incluidos na compilacao ou empacotamento.
    */
   @Parameter
   private List<String> includeSources = null;

   /**
    * Arquivos a serem compilados ou empacotados
    */
   @Parameter
   private List<String> sources = null;

   @Parameter(defaultValue = "${project}", readonly = true, required = true)
   private MavenProject project;

   /**
    * Local maven repository.
    */
   @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
   private ArtifactRepository localRepository;

   @Parameter(defaultValue = "${session}", readonly = true)
   private MavenSession session;

   @Parameter
   private String extractionDirectory;

   /**
    * Define o classifier do artefato.
    */
   @Parameter
   private String classifier;

   /**
    * Diretorio padrao para geracao de arquivos de build
    */
   @Parameter
   private String directory;

   /**
    * Path para os codigos fontes
    */
   @Parameter
   private String sourceDirectory;

   /**
    * Path para os codigos fontes de teste
    */
   @Parameter
   private String testSourceDirectory;

   /**
    * Path para os geracao de arquivos de build
    */
   @Parameter
   private String outputDirectory;

   /**
    * Path para os geracao de arquivos de build de teste
    */
   @Parameter
   private String testOutputDirectory;

   /**
    * Linha de comando a ser executada para preparacao do ambiente de compilacao, linkedicao...
    */
   @Parameter
   private String callForPrepare;

   @Parameter(defaultValue = "false", property = "debug")
   private boolean debug;

   @Parameter(defaultValue = "false", property = "verbose")
   private String verbose;

   @Parameter(defaultValue = "false", property = "showFilename")
   private boolean showFilename;

   @Parameter(defaultValue = "false", property = "force")
   private boolean force;

   @Parameter(defaultValue = "false", property = "parallel")
   private String parallel;

   @Parameter(defaultValue = "false", property = "batch")
   private boolean batch;

   @Parameter(defaultValue = "false", property = "skipTest")
   private boolean skipTest;

   @Component
   private ArtifactResolver artifactResolver;

   @Component
   private ProjectBuilder projectBuilder;

   @Component
   private RepositorySystem repositorySystem;

   private MavenBuildContext buildContext = null;

   private Executor executor = null;

   private DependencyResolver dependencyResolver = null;

   private Properties executionProperties = null;

   protected boolean isProcessingTestSources() {
      return false;
   }

   /**
    * @return the project
    */
   protected MavenProject getProject() {
      return project;
   }

   /**
    * @return the localRepository
    */
   public ArtifactRepository getLocalRepository() {
      return localRepository;
   }

   /**
    * @return the session
    */
   public MavenSession getMavenSession() {
      return session;
   }

   protected String pathToExtractArtifact(Artifact artifact) {
      return BuildUtil.pathToExtractArtifact(getBuildContext().getExtractionDirectory().getAbsolutePath(), artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
   }

   protected void createArtifactExtractionDirectory(final Artifact artifact, final File dirExtract) throws MojoExecutionException {
      /* Se diretorio nao existe, tenta criar... */
      if (!BuildUtil.createArtifactExtractionDirectory(new MavenLogWrapper(getLog()), artifact.getFile(), dirExtract)) {
         throw new MojoExecutionException("Erro ao atualizar a dependencia " + artifact.getId() + ".");
      }
   }

   protected File createArtifactExtractionDirectory(Artifact artifact) throws MojoExecutionException {
      File dirExtract = new File(pathToExtractArtifact(artifact));
      createArtifactExtractionDirectory(artifact, dirExtract);
      return dirExtract;
   }

   /**
    * @return the sources
    */
   public List<String> getSources() {
      return sources;
   }

   /**
    * @param sources the sources to set
    */
   public void setSources(List<String> sources) {
      this.sources = sources;
   }

   /**
    * @return the excludeSources
    */
   public List<String> getExcludeSources() {
      return excludeSources;
   }

   /**
    * @param excludeSources the excludeSources to set
    */
   public void setExcludeSources(List<String> excludeSources) {
      this.excludeSources = excludeSources;
   }

   /**
    * @return the includeSources
    */
   public List<String> getIncludeSources() {
      return includeSources;
   }

   /**
    * @param includeSources the includeSources to set
    */
   public void setIncludeSources(List<String> includeSources) {
      this.includeSources = includeSources;
   }

   /**
    * @return the toolsMapping
    */
   public List<ToolMapping> getToolsMapping() {
      if (toolsMapping != null) {
         return Collections.unmodifiableList(toolsMapping);
      }
      return null;
   }

   public void setBuildContext(MavenBuildContext buildContext) {
      this.buildContext = buildContext;
      getProject().getProperties().put(BUILD_CONTEXT, buildContext);
   }

   public MavenBuildContext getBuildContext() {
      if (buildContext == null) {
         buildContext = (MavenBuildContext) getProject().getProperties().get(BUILD_CONTEXT);
         if (buildContext == null) {
            setBuildContext(new MavenBuildContext());
         }
      }
      return buildContext;
   }

   /**
    * @return the extractionDirectory
    */
   public String getExtractionDirectory() {
      return extractionDirectory;
   }

   private DependencyResolver getDependencyResolver() {
      if (dependencyResolver == null) {
         dependencyResolver = new DependencyResolver(repositorySystem, localRepository, getLog());
      }
      return dependencyResolver;
   }

   protected Artifact resolveDependency(final MavenProject project, Dependency dependency) throws MojoExecutionException {
      return getDependencyResolver().resolveDependency(project, dependency);
   }

   protected List<Artifact> resolveDirectDependencies(MavenProject project, Artifact artifact) throws MojoExecutionException {
      return getDependencyResolver().resolveDependencies(project, artifact);
   }

   /**
    * @return the classifiers
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

   /**
    * @return the artifactResolver
    */
   public ArtifactResolver getArtifactResolver() {
      return artifactResolver;
   }

   /**
    * @return the projectBuilder
    */
   public ProjectBuilder getProjectBuilder() {
      return projectBuilder;
   }

   public Properties getExecutionProperties() {
      if (executionProperties == null) {
         executionProperties = new Properties();
         executionProperties.putAll(getMavenSession().getSystemProperties());
         executionProperties.putAll(getMavenSession().getUserProperties());
         setMojoParameters(executionProperties);
      }

      return executionProperties;
   }

   private void setMojoParameters(Properties properties) {
      if (! properties.containsKey("debug")) {
         properties.setProperty("debug", Boolean.toString(isDebug()));
      }

      if (! properties.containsKey("verbose")) {
         properties.setProperty("verbose", getVerbose());
      }

      if (! properties.containsKey("showFilename")) {
         properties.setProperty("showFilename", Boolean.toString(isShowFilename()));
      }

      if (! properties.containsKey("force")) {
         properties.setProperty("force", Boolean.toString(isForce()));
      }

      if (! properties.containsKey("parallel")) {
         properties.setProperty("parallel", getParallel());
      }

      if (! properties.containsKey("batch")) {
         properties.setProperty("batch", Boolean.toString(isBatch()));
      }

      if (! properties.containsKey("skipTest")) {
         properties.setProperty("skipTest", Boolean.toString(isSkipTest()));
      }
   }

   public String getOption(String prop) {
      return getExecutionProperties().getProperty(prop, "false");
   }

   public String getOption(String prop, String defaultValue) {
      return getExecutionProperties().getProperty(prop, defaultValue);
   }

   @Override
   public void execute(Runnable command) {
      command.run();
   }

   protected Executor getExecutor() {
      if (executor == null) {
         executor = getBuildContext().getExecutor();
         if (executor == null) {
            executor = this;
         }
      }
      return executor;
   }

   /**
    * @return the directory
    */
   public String getDirectory() {
      return directory;
   }

   /**
    * @param directory the directory to set
    */
   public void setDirectory(String directory) {
      this.directory = directory;
   }

   /**
    * @return the sourceDirectory
    */
   public String getSourceDirectory() {
      return sourceDirectory;
   }

   /**
    * @return the testSourceDirectory
    */
   public String getTestSourceDirectory() {
      return testSourceDirectory;
   }

   /**
    * @return the outputDirectory
    */
   public String getOutputDirectory() {
      return outputDirectory;
   }

   /**
    * @return the testOutputDirectory
    */
   public String getTestOutputDirectory() {
      return testOutputDirectory;
   }

   public RepositorySystem getRepositorySystem() {
      return repositorySystem;
   }

   /**
    * @return the debug
    */
   public boolean isDebug() {
      return debug;
   }

   /**
    * @param debug the debug to set
    */
   public void setDebug(boolean debug) {
      this.debug = debug;
   }

   /**
    * @return the verbose
    */
   public String getVerbose() {
      return verbose;
   }

   /**
    * @param verbose the verbose to set
    */
   public void setVerbose(String verbose) {
      this.verbose = verbose;
   }

   /**
    * @return the showFilename
    */
   public boolean isShowFilename() {
      return showFilename;
   }

   /**
    * @param showFilename the showFilename to set
    */
   public void setShowFilename(boolean showFilename) {
      this.showFilename = showFilename;
   }

   /**
    * @return the force
    */
   public boolean isForce() {
      return force;
   }

   /**
    * @param force the force to set
    */
   public void setForce(boolean force) {
      this.force = force;
   }

   /**
    * @return the parallel
    */
   public String getParallel() {
      return parallel;
   }

   /**
    * @param parallel the parallel to set
    */
   public void setParallel(String parallel) {
      this.parallel = parallel;
   }

   /**
    * @return the batch
    */
   public boolean isBatch() {
      return batch;
   }

   /**
    * @param batch the batch to set
    */
   public void setBatch(boolean batch) {
      this.batch = batch;
   }

   /**
    * @return the skipTest
    */
   public boolean isSkipTest() {
      return skipTest;
   }

   /**
    * @param skipTest the skipTest to set
    */
   public void setSkipTest(boolean skipTest) {
      this.skipTest = skipTest;
   }
   
   /**
    * @return a linha de comando a ser executada para preparacao do ambiente de construcao da ferramenta
    */
   public String getCallForPrepare() {
      return callForPrepare;
   }
   
   protected boolean hasError(final String str) {
      return ERROR_PATTERN.matcher(str).find();
   }
   
   protected boolean hasWarning(final String str) {
      return WARNING_PATTERN.matcher(str).find();
   }
   
   protected boolean hasWarnings(ExecutionResult result) {
      for (String out : result.getOutput()) {
         if (WARNING_PATTERN.matcher(out).find()) {
            return true;
         }
      }
      return false;
   }

}
