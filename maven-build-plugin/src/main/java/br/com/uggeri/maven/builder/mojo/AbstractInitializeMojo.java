/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.mojo;

import br.com.uggeri.build.command.CaptureConsumer;
import br.com.uggeri.build.tools.ArtifactDependency;
import br.com.uggeri.build.tools.AbstractExternalTool;
import br.com.uggeri.build.tools.Tool;
import br.com.uggeri.build.tools.BuildUtil;
import static br.com.uggeri.build.tools.BuildUtil.configDirectory;
import br.com.uggeri.build.tools.ExecutionRequest;
import br.com.uggeri.build.tools.ExecutionResult;
import br.com.uggeri.build.tools.compiler.Compiler;
import br.com.uggeri.build.tools.packer.Packer;
import br.com.uggeri.maven.builder.MavenLogWrapper;
import br.com.uggeri.build.tools.PackagingType;
import br.com.uggeri.build.tools.ToolConfig;
import br.com.uggeri.maven.builder.file.SourceFileScanException;
import br.com.uggeri.maven.builder.file.SourceFileScanner;
import br.com.uggeri.maven.builder.file.SourceFileScannerListener;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 */
public abstract class AbstractInitializeMojo extends AbstractSicrediMojo {

   private final static String START_PARSING_MARK = "-=-=-=ENVIRONMENT VARS-=-=-=";

   private Map<String, String> preparedEnvVars = null;

   protected void initialize() throws MojoExecutionException {
      initializeMojoConfigurations();
      defineProjectClassifier();
      defineDefaultDirectories();
      loadMappedTools();
      createDefaultDirectories();
      initializeTools();
      loadSourceFiles();
      loadTestSourceFiles();
      configureExecutor();
      prepareEnvironment();
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
         throw new MojoExecutionException("Tipo informado na tag <Packaging> nao suportado.");
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
      getLog().info("Verificando diretorios de trabalho...");
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
            getBuildContext().setExtractionDirectory(new File(getExtractionDirectory()));
         }
      }
   }

   private void loadMappedTools() throws MojoExecutionException {
      if (getBuildContext().getTools() == null) {
         getLog().info("Verificando ferramentas mapeadas...");
         List<ExternalToolMapped> mappedTools = new ArrayList<ExternalToolMapped>();
         if (getToolsMapping() != null) {
            for (ToolMapping mappedTool : getToolsMapping()) {
               final Artifact toolArtifact;
               final Artifact exeArtifact;
               validateArtifactDependency("tool", mappedTool.getTool());
               toolArtifact = resolveDependency(getProject(), createDependency(mappedTool.getTool()));
               if (toolArtifact == null) {
                  throw new MojoExecutionException("Nao foi possivel resolver a ferramenta configurada no plugin maven-builder-plugin: " + mappedTool.getTool().toString());
               } else if (toolArtifact.getFile() == null) {
                  throw new MojoExecutionException("Arquivo nulo para o artefato: " + mappedTool.getTool().toString());
               } else if (!toolArtifact.getFile().isFile()) {
                  throw new MojoExecutionException("Arquivo '" + toolArtifact.getFile() + "' nao encontrado para o artefato: " + mappedTool.getTool().toString());
               }
               if (mappedTool.getExecutable() != null) {
                  validateArtifactDependency("executable", mappedTool.getTool());
                  exeArtifact = resolveDependency(getProject(), createDependency(mappedTool.getExecutable()));
                  if (exeArtifact == null) {
                     throw new MojoExecutionException("Nao foi possivel resolver o executavel informado no mapeamento de ferramentas para o plugin maven-builder-plugin: " + mappedTool.getExecutable().toString());
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
         Map<String, Compiler> compilers = new HashMap<String, Compiler>();
         Map<String, Packer> packers = new HashMap<String, Packer>();
         getLog().info("Iniciando ferramentas mapeadas...");
         if (getBuildContext().getTools() != null) {
            for (Tool<? extends ExecutionResult, ? extends ExecutionRequest> tool : getBuildContext().getTools()) {
               switch (tool.getToolType()) {
                  case COMPILER:
                     for (String ext : tool.supportedTypes()) {
                        compilers.put(ext.trim().toLowerCase(), (Compiler) tool);
                        getLog().debug("Compilador registrado para arquivos " + ext.trim());
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
               throw new MojoExecutionException("Nenhum compilador configurado para o projeto.");
            }
            /* Se nenhum empacotador foi configurado assume o default (zip) */
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

   /**
    * Carrega e valida a lista de arquivos a serem compilados
    */
   private void loadSourceFiles() throws MojoExecutionException {
      try {
         List<File> sourceFiles = new ArrayList<File>();
         List<File> filesFound;
         File sourceBaseDir = new File(getBuildContext().getSourceDirectory());
         SourceFileScanner sourceScanner;

         if (getLog().isDebugEnabled()) {
            getLog().debug("Procurando codigos fontes em " + sourceBaseDir.getAbsolutePath() + "...");
         } else {
            getLog().info("Procurando codigos fontes...");
         }

         if (getSources() == null && PackagingType.FILE.equals(getBuildContext().getPackagingType())) {
            throw new MojoExecutionException("Nenhum arquivo informado. Informe os arquivos que devem gerar artefatos entre as tags\n"
                    + "<SOURCES>\n"
                    + "<SOURCE>file1</SOURCE>\n"
                    + "<SOURCE>file2</SOURCE>\n"
                    + "...\n"
                    + "<SOURCE>fileN</SOURCE>\n"
                    + "</SOURCES>");
         }

         sourceScanner = new SourceFileScanner(sourceBaseDir, getSources(), getIncludeSources(), getExcludeSources());
         sourceScanner.setListener(new SourceFileScannerListenerImpl());
         filesFound = sourceScanner.scanFiles();

         /*
          * Percorre a lista de fontes, verificando se eh um arquivo, se ele
          * existe e se foi informado nas dependencias um compilador para seu
          * tipo
          */
         for (File fileSource : filesFound) {
            if (!fileSource.exists()) {
               throw new MojoExecutionException("Arquivo " + BuildUtil.canonicalPathName(fileSource) + " nao encontrado.");

            } else if (!fileSource.isFile()) {
               throw new MojoExecutionException(BuildUtil.canonicalPathName(fileSource) + " nao e um arquivo.");

            } else if ((PackagingType.FILE.equals(getBuildContext().getPackagingType())
                    || getBuildContext().isNeedCompilation(fileSource)
                    || getBuildContext().isCanPack(fileSource)) && !sourceFiles.contains(fileSource)) {
               sourceFiles.add(fileSource);

            } else if (getLog().isDebugEnabled()) {
               getLog().debug(BuildUtil.getRelativePath(sourceBaseDir, fileSource) + " ignorado!");
            }
         }
         setSources(BuildUtil.fileListToStringList(sourceBaseDir, sourceFiles));
         getBuildContext().setSourceFiles(sourceFiles);
      } catch (SourceFileScanException ex) {
         throw new MojoExecutionException("Erro ao procurar códigos fontes.", ex);
      }
   }

   private void loadTestSourceFiles() throws MojoExecutionException {
      try {
         List<File> sourceFiles = new ArrayList<File>();
         List<File> filesFound;
         File sourceBaseDir = new File(getBuildContext().getTestSourceDirectory());
         SourceFileScanner sourceScanner;

         if (getLog().isDebugEnabled()) {
            getLog().debug("Procurando codigos fontes de teste em " + sourceBaseDir.getAbsolutePath() + "...");
         } else {
            getLog().info("Procurando codigos fontes de teste...");
         }
         /*
          * Carrega os nomes de todos arquivos encontrados no diretorio de fontes de teste
          */
         sourceScanner = new SourceFileScanner(sourceBaseDir);
         sourceScanner.setListener(new SourceFileScannerListenerImpl());
         filesFound = sourceScanner.scanFiles();

         /*
          * Percorre a lista de fontes, verificando se eh um arquivo compilavel
          */
         for (File fileSource : filesFound) {
            if (getBuildContext().isNeedCompilation(fileSource)) {
               if ((PackagingType.FILE.equals(getBuildContext().getPackagingType())
                       || getBuildContext().isNeedCompilation(fileSource)
                       || getBuildContext().isCanPack(fileSource)) && !sourceFiles.contains(fileSource)) {
                  sourceFiles.add(fileSource);

               } else if (getLog().isDebugEnabled()) {
                  getLog().debug(BuildUtil.getRelativePath(sourceBaseDir, fileSource) + " ignorado!");
               }
            }
         }
         getBuildContext().setTestSourceFiles(sourceFiles);
      } catch (SourceFileScanException ex) {
         throw new MojoExecutionException("Erro ao procurar os códigos fontes de teste.", ex);
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
               getLog().debug("Copiando o arquivo " + depArtifact.getFile().getAbsolutePath() + " para " + destFile.getAbsolutePath() + ".");
               FileUtils.copyFile(depArtifact.getFile(), destFile);
            }
            files.add(destFile);
         } catch (IOException ex) {
            getLog().debug("Erro ao copiar o arquivo " + depArtifact.getFile().getAbsolutePath() + " para " + destFile.getAbsolutePath() + ".\nException: " + ex.getMessage());
            throw new MojoExecutionException("Erro ao configurar dependencias.", ex);
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
               throw new MojoExecutionException("Erro ao autalizar o classpath para o JAR " + toolMapped.getToolArtifact().getFile().getPath(), ex);
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
                     if (!BuildUtil.extractZip(mappedTool.getExeArtifact().getFile(), exeDirFile.getAbsolutePath(), false, new MavenLogWrapper(getLog()))) {
                        throw new MojoExecutionException("Erro ao descompactar o artefato " + mappedTool.getExeArtifact().getId() + ".");
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
         throw new MojoExecutionException("Erro no mapeamento de ferramentas. Nao foi possivel criar a instancia da classe ", ex);
      } catch (IllegalAccessException ex) {
         throw new MojoExecutionException("Erro no mapeamento de ferramentas. Sem acesso para criar a instancia da classe ", ex);
      } catch (ClassNotFoundException ex) {
         throw new MojoExecutionException("Erro no mapeamento de ferramentas. Nao foi possivel localizar a classe ", ex);
      }
      return tools;
   }

   private void configureExecutor() {
      final String parallel = getParallel();
      if (!parallel.equalsIgnoreCase("false")) {
         int threads;
         try {
            threads = Integer.parseInt(parallel);
         } catch (NumberFormatException ex) {
            threads = Runtime.getRuntime().availableProcessors();
         }
         if (threads > 1) {
            getBuildContext().setParallelThreads(threads);
            getBuildContext().setExecutor(new ThreadPoolExecutor(threads, threads, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));
         }
      }
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
      getLog().info("Preparando ambiente...");
      try {
         final ParseEnvironmentVariablesConsumer stdoutConsumer = new ParseEnvironmentVariablesConsumer();
         final CaptureConsumer stderrConsumer = new CaptureConsumer();
         int exitCode;
         final File batFile = createTemporaryBat();
         final Commandline cmd = new Commandline();

         cmd.setExecutable(batFile.getAbsolutePath());
         exitCode = CommandLineUtils.executeCommandLine(cmd, stdoutConsumer, stderrConsumer);
         if (exitCode != 0 || getLog().isDebugEnabled() || !getOption("verbose").equalsIgnoreCase("false")) {
            if (!stderrConsumer.getLines().isEmpty() || !stdoutConsumer.getLines().isEmpty()) {
               getLog().info("________________________________________________________________________");
               for (String msg : stderrConsumer.getLines()) {
                  getLog().info("| " + msg);
               }
               for (String msg : stdoutConsumer.getLines()) {
                  getLog().info("| " + msg);
               }
               getLog().info("________________________________________________________________________");
            }
         }
         preparedEnvVars = stdoutConsumer.getEnvVars();
      } catch (CommandLineException ex) {
         getLog().debug("Exception: " + ex.getMessage());
      } catch (IOException ex) {
         getLog().debug("Exception: " + ex.getMessage());
      }
   }

   private File createTemporaryBat() throws IOException {
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

   public Map<String, String> getPreparedEnvVars() {
      return preparedEnvVars;
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
            } catch (NoClassDefFoundError e) {
               ex = e;

               if (parent != null) {
                  c = parent.loadClass(className);
               }
            } catch (ClassNotFoundException e) {
               ex = e;

               if (parent != null) {
                  c = parent.loadClass(className);
               }
            }
         }

         if (c == null) {
            throw new ClassNotFoundException("Classe nao encontrada.", ex);
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

   private class SourceFileScannerListenerImpl implements SourceFileScannerListener {

      public SourceFileScannerListenerImpl() {
      }
      int level = 0;

      @Override
      public void dirScanStarted(File dir) {
         if (getLog().isDebugEnabled()) {
            getLog().debug(StringUtils.repeat(" ", level * 2) + dir.getName() + "...");
         }
         ++level;
      }

      @Override
      public void dirScanFinished(File dir) {
         --level;
      }

      @Override
      public void fileFound(File file) {
         if (getLog().isDebugEnabled()) {
            getLog().debug(StringUtils.repeat(" ", (level + 1) * 2) + file.getName() + " encontrado.");
         }
      }

      @Override
      public void fileIncluded(File file) {
      }

      @Override
      public void fileDismissed(File file) {
         getLog().info("Arquivo excluído do build: " + file.getAbsolutePath());
      }
   }

   private class ParseEnvironmentVariablesConsumer extends CaptureConsumer {

      private boolean startParsing = false;
      private final Map<String, String> envVars = new HashMap<String, String>();
      private final List<String> lines = new ArrayList<String>();

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
