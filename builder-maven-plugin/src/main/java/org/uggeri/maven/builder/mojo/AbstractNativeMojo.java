/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.ExecutionResult;
import org.uggeri.maven.builder.MavenLogWrapper;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import org.uggeri.build.tools.ArtifactDependency;
import org.uggeri.build.tools.PackagingType;
import org.uggeri.build.tools.packer.Packer;
import org.uggeri.maven.builder.mojo.compile.ConditionalDefine;

/**
 *
 * @author ADMIN
 */
public abstract class AbstractNativeMojo extends AbstractMojo implements Executor {

   private static final String PROPERTY_DEFINE = "def.";

   private static final Pattern WARNING_PATTERN = Pattern.compile("(^|[^a-zA-Z])(warn|warning)([^a-zA-Z]|$)", Pattern.CASE_INSENSITIVE);

   private static final Pattern ERROR_PATTERN = Pattern.compile("(^|[^a-zA-Z])error([^a-zA-Z]|$)", Pattern.CASE_INSENSITIVE);
   
   private static Executor executor = null;

   private static int parallelThreads = 1;
   

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

   @Parameter
   private String pathSeparator = File.separator;

   /**
    * Defines condicionais. Declarados somente se uma condicao for satisfeita.
    */
   @Parameter
   protected List<ConditionalDefine> conditionalDefines;

   @Parameter
   protected Map<String, String> defines;

   @Parameter
   private String includesPaths;

   @Parameter
   private String librariesPaths;

   @Parameter
   private String libraries;

   @Component
   private ArtifactResolver artifactResolver;

   @Component
   private ProjectBuilder projectBuilder;

   @Component
   private RepositorySystem repositorySystem;

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
         throw new MojoExecutionException("Error updating dependency " + artifact.getId() + ".");
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

   public MavenBuildContext getBuildContext() {
      return MavenBuildContext.getInstance();
   }

   /**
    * @return the extractionDirectory
    */
   public String getExtractionDirectory() {
      return BuildUtil.platformPath(extractionDirectory);
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

   protected void configureExecutor() {
      if (executor == null) {
         final String parallelValue = getParallel();
         if (!parallelValue.equalsIgnoreCase("false")) {
            int threads;
            try {
               threads = Integer.parseInt(parallelValue);
            } catch (NumberFormatException ex) {
               threads = Runtime.getRuntime().availableProcessors();
            }
            if (threads > 1) {
               executor = new ThreadPoolExecutor(threads, threads, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
               parallelThreads = threads;
            } else {
               executor = this;
            }
         }
      }
   }

   protected Executor getExecutor() {
      if (executor == null) {
         configureExecutor();
      }
      return executor;
   }

   public int getParallelThreads() {
      return parallelThreads;
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
      this.directory = BuildUtil.platformPath(directory);
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

   /**
    * @return the conditional defines
    */
   public List<ConditionalDefine> getConditonalDefines() {
      return conditionalDefines;
   }

   /**
    * @return the defines
    */
   public Map<String, String> getDefines() {
      if (defines == null) {
         defines = new HashMap<>();
      }
      return defines;
   }

   private boolean isPackagingType(String type, PackagingType[] types) {
      for (PackagingType tpPck : types) {
         if (type.equalsIgnoreCase(tpPck.toString())) {
            return true;
         }
      }
      return false;
   }

   public String getPathSeparator() {
      return pathSeparator;
   }

   private Map<String, Artifact> mapDependenciesArtifacts(final MavenProject project, final PackagingType... types) throws MojoExecutionException {
      Map<String, Artifact> mapArtifacts = new HashMap<>();
      for (Dependency dep : project.getDependencies()) {
         if (types.length == 0 || isPackagingType(dep.getType(), types)) {
            final Artifact a = getDependencyResolver().resolveDependency(project, dep);
            getLog().debug("Resolved dependency: " + dep.getManagementKey());
            mapArtifacts.put(a.getGroupId() + ":" + a.getArtifactId(), a);
         }
      }
      return mapArtifacts;
   }

   private List<Artifact> orderedArtifacts(final MavenProject project, final Map<String, Artifact> mapArtifacts) throws MojoExecutionException {
      List<Artifact> artifacts = new ArrayList<>();
      List<Dependency> dependencies = project.getDependencies();
      for (Dependency dep : dependencies) {
         Artifact depArtifact = mapArtifacts.get(dep.getGroupId() + ":" + dep.getArtifactId());
         if (depArtifact != null) {
            artifacts.add(depArtifact);
         } else {
            getLog().debug("Dependency " + dep + " not found in artifacts.");
         }
      }
      return artifacts;
   }

   private List<Artifact> listOrderedLibraries() throws MojoExecutionException {
      final Map<String, Artifact> mappedArtifacts = mapDependenciesArtifacts(getProject(), PackagingType.LIB, PackagingType.FILE);
      return orderedArtifacts(getProject(), mappedArtifacts);
   }

   private List<Artifact> listOrderedIncludes() throws MojoExecutionException {
      final List<Artifact> includesArtifacts = new ArrayList<>();
      final Map<String, Artifact> mappedArtifacts = mapDependenciesArtifacts(getProject(), PackagingType.INCLUDE, PackagingType.FILE);
      for (Artifact artifact : orderedArtifacts(getProject(), mappedArtifacts)) {
         includesArtifacts.add(artifact);
         if (artifact.getType().equalsIgnoreCase(PackagingType.INCLUDE.toString())) {
            includesArtifacts.addAll(resolveTransitiveIncludes(mappedArtifacts, artifact));
         }
      }
      return includesArtifacts;
   }

   private List<Artifact> resolveTransitiveIncludes(Map<String, Artifact> mappedArtifacts, Artifact artifact) throws MojoExecutionException {
      final List<Artifact> includesArtifacts = new ArrayList<>();
      for (final Artifact a : resolveDirectDependencies(getProject(), artifact)) {
         final String key = a.getGroupId() + ":" + a.getArtifactId();
         if (artifact.getType().equalsIgnoreCase(PackagingType.INCLUDE.toString()) 
                 && ! mappedArtifacts.containsKey(key)) {
            mappedArtifacts.put(key, a);
            includesArtifacts.add(a);
            includesArtifacts.addAll(resolveTransitiveIncludes(mappedArtifacts, a));
         }
      }
      return includesArtifacts;
   }

   protected List<String> listIncludesPaths() throws MojoExecutionException {
      final List<Artifact> orderedArtifacts = listOrderedIncludes();
      final List<String> unpackedPaths = ArtifactUtil.unpackIncludes(orderedArtifacts, getBuildContext(), isForce(), new MavenLogWrapper(getLog())); 
      final List<String> includes = new ArrayList<>(unpackedPaths);

      if (includesPaths != null) {
         final String paths[] = includesPaths.split(";");
         for (final String path : paths) {
            final String adjustedPath = BuildUtil.platformPath(path);
            final File dir = new File(adjustedPath);
            if (dir.isDirectory()) {
               includes.add(dir.getAbsolutePath());
            } else {
               getLog().warn("Path set in <includesPaths> option is not a directory: " + adjustedPath);
            }
         }
      }
      if (getLog().isDebugEnabled()) {
         getLog().debug("Compilation request includes: " + includes);
      }
      return includes;
   }

   protected Map<String, String> mapDefines() {
      final Map<String, String> definesMapped = new HashMap<>();
      for (Map.Entry<String, String> define : getDefines().entrySet()) {
         definesMapped.put(define.getKey(), define.getValue());
         getLog().debug("DEFINE: " + define.getKey() + "=" + define.getValue());
      }
      if (getConditonalDefines() != null) {
         Set<ArtifactDependency> dependencies = ArtifactUtil.artifactsDependencies(getProject());
         for (ConditionalDefine define : getConditonalDefines()) {
            if (define.isConditionSatisfied(dependencies, getProject().getProperties())) {
               definesMapped.put(define.getDefine(), define.getValue());
               getLog().debug("DEFINE: " + define.getDefine() + "=" + define.getValue());
            } else {
               getLog().debug("NOT DEFINED: " + define.getDefine() + "=" + define.getValue());
            }
         }
      }
      for (Map.Entry<Object, Object> prop : getProject().getProperties().entrySet()) {
         final String key = prop.getKey().toString();
         if (key.startsWith(PROPERTY_DEFINE) && key.length() > PROPERTY_DEFINE.length()) {
            definesMapped.put(key.substring(4), prop.getValue() != null ? prop.getValue().toString() : null);
            getLog().debug("PROPERTY DEFINE: " + key + "=" + prop.getValue());
         }
      }
      return definesMapped;
   }

   protected List<File> listLibraries(final Packer packer, final List<File> inputFiles, final boolean scopeTest) throws MojoExecutionException {
      final List<File> librariesFiles = new ArrayList<>();
      final Collection<Artifact> orderedArtifacts = listOrderedLibraries();
      for (Artifact artifact : orderedArtifacts) {
         if (scopeTest || ! Artifact.SCOPE_TEST.equalsIgnoreCase(artifact.getScope())) {
            if (artifact.getType().equals(PackagingType.LIB.toString())) {
               getLog().debug("Library dependency found: " + artifact.getId());
               librariesFiles.add(artifact.getFile());
            } else if (artifact.getType().equals(PackagingType.FILE.toString())) {
               if (packer.supportedTypes().contains(BuildUtil.fileExtension(artifact.getArtifactId()))) {
                  getLog().debug("File dependency found: " + artifact.getId());
                  inputFiles.add(artifact.getFile());
               }
            }
         }
      }
      loadPluginConfigLibraries(packer, inputFiles, librariesFiles);
      return librariesFiles;
   }

   private void loadPluginConfigLibraries(Packer packer, List<File> inputFiles, final List<File> librariesFiles) throws MojoExecutionException {
      final List<File> searchLibs = new ArrayList<>();
      if (librariesPaths != null) {
         final String paths[] = librariesPaths.split(";");
         for (final String path : paths) {
            final String adjustedPah = BuildUtil.platformPath(path);
            getLog().debug("Path configured in <librariesPaths>: '" + adjustedPah + "'");
            if (! adjustedPah.trim().isEmpty()) {
               final File dir = new File(adjustedPah);
               if (dir.isDirectory()) {
                  searchLibs.add(dir);
               } else {
                  getLog().warn("Path in <librariesPaths> is not a directory: " + adjustedPah);
               }
            }
         }
         if (!searchLibs.isEmpty()) {
            final String libNames[] = libraries.split(";");
            for (final String libName : libNames) {
               if (!libName.trim().isEmpty()) {
                  boolean found = false;
                  for (final File searchDir : searchLibs) {
                     final File libFile = new File(searchDir, libName.trim());
                     if (libFile.isFile()) {
                        if (packer.supportedTypes().contains(BuildUtil.fileExtension(libFile))) {
                           inputFiles.add(libFile);
                           getLog().debug("Object found in <libraries>: " + libFile + ".");
                        } else {
                           librariesFiles.add(libFile);
                           getLog().debug("Library found in <libraries>: " + libFile + ".");
                        }
                        found = true;
                        break;
                     }
                  }
                  if (!found) {
                     getLog().warn("Library set in <libraries> not found: " + libName);
                  }
               }
            }
         } else if (libraries != null) {
            getLog().warn("No valid directory in <librariesPaths>.");
         }
      } else if (libraries != null) {
         throw new MojoExecutionException("Option <librariesPaths> not set, but is necessary to use <libraries> option.");
      }
   }
}
