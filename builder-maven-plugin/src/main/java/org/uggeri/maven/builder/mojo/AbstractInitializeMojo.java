/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo;

import org.uggeri.build.command.CaptureConsumer;
import org.uggeri.build.tools.ArtifactDependency;
import org.uggeri.build.tools.AbstractExternalTool;
import org.uggeri.build.tools.Tool;
import org.uggeri.build.tools.BuildUtil;
import static org.uggeri.build.tools.BuildUtil.configDirectory;
import org.uggeri.build.tools.ExecutionRequest;
import org.uggeri.build.tools.ExecutionResult;
import org.uggeri.build.tools.compiler.Compiler;
import org.uggeri.build.tools.packer.Packer;
import org.uggeri.maven.builder.MavenLogWrapper;
import org.uggeri.build.tools.PackagingType;
import org.uggeri.build.tools.ToolConfig;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 */
public abstract class AbstractInitializeMojo extends AbstractNativeMojo {

   private final static String START_PARSING_MARK = "-=-=-=ENVIRONMENT VARS-=-=-=";

   private Map<String, String> preparedEnvVars = null;

   protected void initialize() throws MojoExecutionException {
      MavenBuildContext.initialize();
      initializeMojoConfigurations();
      defineProjectClassifier();
      defineDefaultDirectories();
      loadMappedTools();
      createDefaultDirectories();
      initializeTools();
      configureExecutor();
      prepareEnvironment();
      unpackZipDependencies();
   }

   private void defineProjectClassifier() {
      if (getBuildContext().getClassifier() == null) {
         if (getClassifier() != null && !getClassifier().isEmpty()) {
            getBuildContext().setClassifier(getClassifier());
         }
      }
   }

   protected void initializeMojoConfigurations() throws MojoExecutionException {
      PackagingType type = PackagingType.valueOf(getProject().getPackaging().toUpperCase());
      if (type == null) {
         throw new MojoExecutionException("Unsupported <Packaging> type.");
      }
      getBuildContext().setPackagingType(type);
   }

   protected void createDefaultDirectories() {
      BuildUtil.makeDirsPath(getBuildContext().getDirectory());
      BuildUtil.makeDirsPath(getBuildContext().getOutputDirectory());
      BuildUtil.makeDirsPath(getBuildContext().getTestOutputDirectory());
      BuildUtil.makeDirsPath(getBuildContext().getExtractionDirectory().getAbsolutePath());
   }

   /**
    * Define os diretorios default utilizados para fontes, objetos e pacotes (exes, libs, zips, etc.)
    */
   protected void defineDefaultDirectories() {
      getLog().info("Defining project directories...");
      /* Diretorio onde sera gerado o artefato */
      getBuildContext().setDirectory(configDirectory(getProject().getBasedir(), getDirectory(), getProject().getBuild().getDirectory()));
      getBuildContext().setSourceDirectory(configDirectory(getProject().getBasedir(), getSourceDirectory(), getProject().getBuild().getSourceDirectory()));
      getBuildContext().setTestSourceDirectory(configDirectory(getProject().getBasedir(), getTestSourceDirectory(), getProject().getBuild().getTestSourceDirectory()));
      getBuildContext().setOutputDirectory(configDirectory(getProject().getBasedir(), getOutputDirectory(), getProject().getBuild().getOutputDirectory()));
      getBuildContext().setTestOutputDirectory(configDirectory(getProject().getBasedir(), getTestOutputDirectory(), getProject().getBuild().getTestOutputDirectory()));
      /* Diretorio onde serao extraidas as dependencias */
      if (getBuildContext().getExtractionDirectory() == null) {
         if (getExtractionDirectory() == null || getExtractionDirectory().trim().isEmpty()) {
            getBuildContext().setExtractionDirectory(new File(System.getProperty("java.io.tmpdir")));
         } else {
            getBuildContext().setExtractionDirectory(new File(BuildUtil.platformPath(getExtractionDirectory())));
         }
      }
   }

   private void loadMappedTools() throws MojoExecutionException {
      if (getBuildContext().getTools() == null) {
         getLog().info("Looking for mapped tools...");
         List<ExternalToolMapped> mappedTools = new ArrayList<>();
         if (getToolsMapping() != null) {
            for (ToolMapping mappedTool : getToolsMapping()) {
               final Artifact toolArtifact;
               final Artifact exeArtifact;
               validateArtifactDependency("tool", mappedTool.getTool());
               toolArtifact = resolveDependency(getProject(), createDependency(mappedTool.getTool()));
               if (toolArtifact == null) {
                  throw new MojoExecutionException("Unresolved mapped tool in builder-maven-plugin: " + mappedTool.getTool().toString());
               } else if (toolArtifact.getFile() == null) {
                  throw new MojoExecutionException("Null file for artifact: " + mappedTool.getTool().toString());
               } else if (!toolArtifact.getFile().isFile()) {
                  throw new MojoExecutionException("File '" + toolArtifact.getFile() + "' not found for artifact: " + mappedTool.getTool().toString());
               }
               if (mappedTool.getExecutable() != null) {
                  validateArtifactDependency("executable", mappedTool.getTool());
                  exeArtifact = resolveDependency(getProject(), createDependency(mappedTool.getExecutable()));
                  if (exeArtifact == null) {
                     throw new MojoExecutionException("Unresolved executable for mapped tool in plugin builder-maven-plugin: " + mappedTool.getExecutable().toString());
                  }
               } else {
                  exeArtifact = null;
               }
               mappedTools.add(new ExternalToolMapped(mappedTool.getTool().getToolClass(), toolArtifact, exeArtifact, mappedTool.getToolConfig()));
            }
         }
         getBuildContext().setTools(createTools(mappedTools));
      }
   }

   private void initializeTools() throws MojoExecutionException {
      if (!getBuildContext().isToolsInitialized()) {
         Map<String, Compiler> compilers = new HashMap<>();
         Map<String, Packer> packers = new HashMap<>();
         getLog().info("Initializing tools...");
         if (getBuildContext().getTools() != null) {
            for (Tool<? extends ExecutionResult, ? extends ExecutionRequest> tool : getBuildContext().getTools()) {
               switch (tool.getToolType()) {
                  case COMPILER:
                     for (String ext : tool.supportedTypes()) {
                        compilers.put(ext.trim().toLowerCase(), (Compiler) tool);
                        getLog().debug("Compiler registered for files " + ext.trim());
                     }
                     break;
                  case PACKER:
                     for (String packagingType : ((Packer) tool).getSupportedPackagings()) {
                        packers.put(packagingType, (Packer) tool);
                     }
                     break;
               }
            }
            if (getBuildContext().getPackagingType().isNeedCompile() && compilers.isEmpty()) {
               throw new MojoExecutionException("No compiler registered for project.");
            }
            /* Se nenhum empacotador foi configurado assume o default (unzip) */
            if (!packers.containsKey(getBuildContext().getPackagingType().toString()) && getBuildContext().getPackagingType().isNeedPackage()) {
               packers.put(getBuildContext().getPackagingType().toString(), getBuildContext().getPackagingType().getDefaultPacker());
            }
            getBuildContext().setCompilers(compilers);
            getBuildContext().setPackers(packers);
            /* TODO: Modificar para que seja uma dependencia externa. Ou seja, precise ser colocado como
             dependencia do plugin e mapeado como as demais. Nao deve existir o default packer. */
            getBuildContext().setToolsInitialized(true);
         }
      }
   }

   private String targetName(Artifact artifact) {
      if (artifact.getType().equals(PackagingType.FILE.toString())) {
         return artifact.getArtifactId();
      } else if (artifact.getArtifactId().endsWith("-" + artifact.getType())) {
         return artifact.getArtifactId().replace("-" + artifact.getType(), "." + artifact.getType());
      } else {
         return artifact.getArtifactId() + "." + artifact.getType();
      }
   }

   private Collection<File> toolFilesDependencies(final Artifact artifact) throws MojoExecutionException {
      List<File> files = new ArrayList<File>();
      Collection<Artifact> dependentArtifacts = resolveDirectDependencies(getProject(), artifact);
      for (Artifact depArtifact : dependentArtifacts) {
         File dirExtract = createArtifactExtractionDirectory(depArtifact);
         File destFile = new File(dirExtract.getAbsolutePath(), targetName(depArtifact));
         try {
            if (!destFile.exists() || depArtifact.getFile().lastModified() > destFile.lastModified()) {
               getLog().debug("Copying file " + depArtifact.getFile().getAbsolutePath() + " to " + destFile.getAbsolutePath() + ".");
               FileUtils.copyFile(depArtifact.getFile(), destFile);
            }
            files.add(destFile);
         } catch (IOException ex) {
            getLog().debug("Error copying the file " + depArtifact.getFile().getAbsolutePath() + " to " + destFile.getAbsolutePath() + ".\nException: " + ex.getMessage());
            throw new MojoExecutionException("Error configuring dependencies.", ex);
         }
      }
      return files;
   }

   private List<Tool> createTools(List<ExternalToolMapped> toolsMapped) throws MojoExecutionException {
      List<Tool> tools = new ArrayList<Tool>();
      try {
         ArtifactClassLoader newLoader = new ArtifactClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
         for (ExternalToolMapped toolMapped : toolsMapped) {
            try {
               newLoader.addURL(toolMapped.getToolArtifact().getFile().toURI().toURL());
            } catch (MalformedURLException ex) {
               throw new MojoExecutionException("Error updating classpath with JAR " + toolMapped.getToolArtifact().getFile().getPath(), ex);
            }
         }
         for (ExternalToolMapped mappedTool : toolsMapped) {
            AbstractExternalTool tool = (AbstractExternalTool) newLoader.loadClass(mappedTool.getClassName()).newInstance();
            tool.setToolConfig(mappedTool.getToolConfig());
            if (mappedTool.getExeArtifact() != null) {
               File exeDirFile;
               if (mappedTool.getExeArtifact().getType().equalsIgnoreCase("app")) {
                  exeDirFile = new File(pathToExtractArtifact(mappedTool.getExeArtifact()));
                  if (!exeDirFile.exists() || exeDirFile.lastModified() < mappedTool.getExeArtifact().getFile().lastModified()) {
                     createArtifactExtractionDirectory(mappedTool.getExeArtifact(), exeDirFile);
                     getLog().info("Extraindo " + mappedTool.getExeArtifact().getId() + "...");
                     if (!BuildUtil.unzip(mappedTool.getExeArtifact().getFile(), exeDirFile.getAbsolutePath(), false, new MavenLogWrapper(getLog()))) {
                        throw new MojoExecutionException("Error unpacking artifact " + mappedTool.getExeArtifact().getId() + ".");
                     }
                  }

               } else {
                  exeDirFile = mappedTool.getExeArtifact().getFile();
               }
               tool.setExecutable(exeDirFile);
               tool.setExecutionDependencies(toolFilesDependencies(mappedTool.getExeArtifact()));
            }
            tools.add(tool);
         }

      } catch (InstantiationException ex) {
         throw new MojoExecutionException("Error mapping tools. Class could not be instantiated ", ex);
      } catch (IllegalAccessException ex) {
         throw new MojoExecutionException("Error mapping tools. Without permission to instantiate class ", ex);
      } catch (ClassNotFoundException ex) {
         throw new MojoExecutionException("Error mapping tools. Class not found ", ex);
      }
      return tools;
   }

   private Dependency createDependency(ArtifactDependency artifactDep) {
      final Dependency dep;
      dep = new Dependency();
      dep.setGroupId(artifactDep.getGroupId());
      dep.setArtifactId(artifactDep.getArtifactId());
      dep.setType(artifactDep.getType());
      dep.setClassifier(artifactDep.getClassifier());
      dep.setVersion(artifactDep.getVersion());
      dep.setScope(artifactDep.getScope());
      return dep;
   }

   private void validateArtifactDependency(String artifactId, ArtifactDependency dep) throws MojoExecutionException {
      if (dep.getGroupId() == null || dep.getGroupId().isEmpty()) {
         throw new MojoExecutionException("Artifact groupId not set for " + artifactId + ".");
      }
      if (dep.getArtifactId() == null || dep.getArtifactId().isEmpty()) {
         throw new MojoExecutionException("Artifact artifactId not set for " + artifactId + ".");
      }
      if (dep.getType() == null || dep.getType().isEmpty()) {
         throw new MojoExecutionException("Artifact type not set for " + artifactId + ".");
      }
      if (dep.getVersion() == null || dep.getVersion().isEmpty()) {
         throw new MojoExecutionException("Artifact version not set for " + artifactId + ".");
      }
   }

   private void prepareEnvironment() {
      preparedEnvVars = System.getenv();
//      getLog().info("Configuring environment...");
//      try {
//         final ParseEnvironmentVariablesConsumer stdoutConsumer = new ParseEnvironmentVariablesConsumer();
//         final CaptureConsumer stderrConsumer = new CaptureConsumer();
//         int exitCode;
//         final Commandline cmd = new Commandline();
//         final File batFile = createTemporaryShellScript();
//
//         batFile.deleteOnExit();
//         cmd.setExecutable(batFile.getAbsolutePath());
//         exitCode = CommandLineUtils.executeCommandLine(cmd, stdoutConsumer, stderrConsumer);
//         if (exitCode != 0 || getLog().isDebugEnabled() || !getOption("verbose").equalsIgnoreCase("false")) {
//            if (!stderrConsumer.getLines().isEmpty() || !stdoutConsumer.getLines().isEmpty()) {
//               getLog().info("________________________________________________________________________");
//               for (String msg : stderrConsumer.getLines()) {
//                  getLog().info("| " + msg);
//               }
//               for (String msg : stdoutConsumer.getLines()) {
//                  getLog().info("| " + msg);
//               }
//               getLog().info("________________________________________________________________________");
//            }
//         }
//         preparedEnvVars = stdoutConsumer.getEnvVars();
//      } catch (CommandLineException | IOException ex) {
//         getLog().debug("Exception: " + ex.getMessage());
//      }
   }

   private File createTemporaryShellScript() throws IOException {
      if (BuildUtil.isWindows()) {
         return createBatScript();
      } else {
         return createLinuxScript();
      }
   }

   private File createBatScript() throws IOException {
      final File batFile = File.createTempFile("_mvnsetenv", ".bat");
      final StringBuilder buffer = new StringBuilder();
      if (getCallForPrepare() != null && !getCallForPrepare().isEmpty()) {
         buffer.append("@echo off").append(System.lineSeparator());
         buffer.append("call \"").append(getCallForPrepare()).append("\"").append(System.lineSeparator());
      }
      buffer.append("@echo on").append(System.lineSeparator());
      buffer.append("echo ").append(START_PARSING_MARK).append(System.lineSeparator());
      buffer.append("set").append(System.lineSeparator());
      FileUtils.fileWrite(batFile.getAbsolutePath(), buffer.toString());
      return batFile;
   }

   private File createLinuxScript() throws IOException {
      final File batFile = File.createTempFile("_mvnsetenv", ".sh");
      final StringBuilder buffer = new StringBuilder();
      if (getCallForPrepare() != null && !getCallForPrepare().isEmpty()) {
         buffer.append(". ").append(getCallForPrepare()).append(System.lineSeparator());
      }
      buffer.append("echo \"").append(START_PARSING_MARK).append('"').append(System.lineSeparator());
      buffer.append("printenv").append(System.lineSeparator());
      FileUtils.fileWrite(batFile.getAbsolutePath(), buffer.toString());
      return batFile;
   }

   public Map<String, String> getPreparedEnvVars() {
      return preparedEnvVars;
   }

   private void unpackZipDependencies() throws MojoExecutionException {
      for (Dependency dep : getProject().getDependencies()) {
         if ("zip".equalsIgnoreCase(dep.getType())) {
            final Artifact artifact = resolveDependency(getProject(), dep);
            getLog().debug("Dependency with ZIP artifact found: " + dep);
            ArtifactUtil.unzipArtifact(getBuildContext(), artifact, isForce(), new MavenLogWrapper(getLog()));
         }
      }
   }

   public class ArtifactClassLoader extends URLClassLoader {

      private ClassLoader parent = null;

      public ArtifactClassLoader(URL[] urls, ClassLoader parent) {
         super(urls, parent);
         this.parent = parent;
      }

      @Override
      public void addURL(URL url) {
         super.addURL(url);
      }

      public void setParent(ClassLoader parent) {
         this.parent = parent;
      }

      @Override
      public synchronized Class loadClass(String className) throws ClassNotFoundException {
         Class c = findLoadedClass(className);

         Throwable ex = null;

         if (c == null) {
            try {
               c = findClass(className);
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
               ex = e;

               if (parent != null) {
                  c = parent.loadClass(className);
               }
            }
         }

         if (c == null) {
            throw new ClassNotFoundException("Class not found.", ex);
         }
         resolveClass(c);
         return c;
      }
   }

   private class ExternalToolMapped {

      private final String className;
      private final Artifact toolArtifact;
      private final Artifact exeArtifact;
      private final ToolConfig toolConfig;

      public ExternalToolMapped(String className, Artifact toolArtifact, Artifact exeArtifact, ToolConfig config) {
         this.className = className;
         this.toolArtifact = toolArtifact;
         this.exeArtifact = exeArtifact;
         this.toolConfig = config;
      }

      /**
       * @return the className
       */
      public String getClassName() {
         return className;
      }

      /**
       * @return the toolArtifact
       */
      public Artifact getToolArtifact() {
         return toolArtifact;
      }

      /**
       * @return the exeArtifact
       */
      public Artifact getExeArtifact() {
         return exeArtifact;
      }

      public ToolConfig getToolConfig() {
         return toolConfig;
      }
   }

   private class ParseEnvironmentVariablesConsumer extends CaptureConsumer {

      private boolean startParsing = false;
      private final Map<String, String> envVars = new HashMap<>();
      private final List<String> lines = new ArrayList<>();

      @Override
      public void consumeLine(String line) {
         lines.add(line);
         if (startParsing) {
            final int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
               final String var = line.substring(0, equalIndex);
               final String value = line.substring(equalIndex + 1);
               envVars.put(var, value);
            }
         } else if (line.equals(START_PARSING_MARK)) {
            startParsing = true;
         }
      }

      @Override
      public Collection<String> getLines() {
         return lines;
      }

      public Map<String, String> getEnvVars() {
         return envVars;
      }
   }
}
