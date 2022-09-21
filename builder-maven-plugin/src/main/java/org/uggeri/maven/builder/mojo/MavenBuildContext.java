/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.mojo;

import org.uggeri.build.tools.Tool;
import org.uggeri.build.tools.BuildUtil;
import static org.uggeri.build.tools.BuildUtil.configDirectory;
import org.uggeri.build.tools.compiler.Compiler;
import org.uggeri.build.tools.packer.Packer;
import org.uggeri.build.tools.PackagingType;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author fabio_uggeri
 */
public class MavenBuildContext {

   private static MavenBuildContext buildContext = new MavenBuildContext();

   private static Map<String, Connection> connections = null;

   private List<Tool> tools = null;

   private File extractionDirectory = null;

   private List<String> sources = null;

   private List<String> includeSources = null;

   private List<String> excludeSources = null;

   private List<File> sourceFiles = null;

   private Map<String, Compiler> compilers = null;

   private Map<String, Packer> packers = null;

   private boolean toolsInitialized = false;

   private PackagingType packagingType = null;

   private String classifier = null;

   private String acronym = null;

   private String systemCode = null;

   private List<File> headerFiles = null;

   private List<File> testSourceFiles = null;

   private String directory;

   private String sourceDirectory;

   private String testSourceDirectory;

   private String outputDirectory;

   private String testOutputDirectory;

   private File executableTestFile = null;

   private Map<String, String> environmentVariables = null;

   public static MavenBuildContext createFromProject(MavenProject project) {
      final MavenBuildContext ctx = new MavenBuildContext();
      final Plugin thisPlugin = project.getPlugin("org.uggeri.maven.builder:builder-maven-plugin");
      if (thisPlugin != null) {
         final Xpp3Dom config = (Xpp3Dom) thisPlugin.getConfiguration();
         ctx.setPackagingType(PackagingType.valueOf(project.getPackaging().toUpperCase()));
         ctx.setClassifier(valueOf(config, "classifier"));
         ctx.configSources(config);
         ctx.configDirectories(project, config);
      }
      return ctx;
   }

   private static String valueOf(Xpp3Dom node, String tag) {
      Xpp3Dom child = node.getChild(tag);
      if (child != null) {
         return child.getValue();
      }
      return null;
   }

   private static List<String> listAnyValuesOf(Xpp3Dom node, String tag) {
      Xpp3Dom tagNode = node.getChild(tag);
      if (tagNode != null) {
         final Xpp3Dom children[] = tagNode.getChildren();
         if (children != null && children.length > 0) {
            final List<String> values = new ArrayList<String>(children.length);
            for (Xpp3Dom child : children) {
               values.add(child.getValue());
            }
            return values;
         }
      }
      return null;
   }
   
   public static void initialize() {
      buildContext = new MavenBuildContext();
   }
   
   public static MavenBuildContext getInstance() {
      if (buildContext == null) {
         initialize();
      }
      return buildContext;
   }

   /**
    * @return the tools
    */
   public List<Tool> getTools() {
      return tools;
   }

   /**
    * @param tools the tools to set
    */
   public void setTools(List<Tool> tools) {
      this.tools = tools;
   }

   /**
    * @return the extractionDirectory
    */
   public File getExtractionDirectory() {
      return extractionDirectory;
   }

   /**
    * @param extractionDirectory the extractionDirectory to set
    */
   public void setExtractionDirectory(File extractionDirectory) {
      this.extractionDirectory = extractionDirectory;
   }

   /**
    * @return the sourceFiles
    */
   public List<File> getSourceFiles() {
      return sourceFiles;
   }

   /**
    * @param sourceFiles the sourceFiles to set
    */
   public void setSourceFiles(List<File> sourceFiles) {
      this.sourceFiles = sourceFiles;
   }

   /**
    * @return the compilers
    */
   public Map<String, Compiler> getCompilers() {
      return compilers;
   }

   /**
    * @param compilers the compilers to set
    */
   public void setCompilers(Map<String, Compiler> compilers) {
      this.compilers = compilers;
   }

   /**
    * @return the packer
    */
   public Map<String, Packer> getPackers() {
      return packers;
   }

   /**
    * @param packers the packer to set
    */
   public void setPackers(Map<String, Packer> packers) {
      this.packers = packers;
   }

   /**
    * @return the toolsInitialized
    */
   public boolean isToolsInitialized() {
      return toolsInitialized;
   }

   /**
    * @param toolsInitialized the toolsInitialized to set
    */
   public void setToolsInitialized(boolean toolsInitialized) {
      this.toolsInitialized = toolsInitialized;
   }

   /**
    * @return the packagingType
    */
   public PackagingType getPackagingType() {
      return packagingType;
   }

   /**
    * @param packagingType the packagingType to set
    */
   public void setPackagingType(PackagingType packagingType) {
      this.packagingType = packagingType;
   }

   /**
    * Verifica se a extensao do arquivo eh suportada por algum compilador mapeado
    *
    * @param fileSource arquivo a ser verificado suporte a compilacao
    * @return  <code>true</code> - se o arquivo precisa ser compilado para ser empacotado<br/> <code>false</code> - se o arquivo pode
    * ser empacotado sem a necessidade de compilacao
    */
   public boolean isNeedCompilation(File fileSource) {
      if (getCompilers() != null) {
         return getCompilers().get(BuildUtil.fileExtension(fileSource).trim().toLowerCase()) != null;
      }
      return false;
   }

   /**
    * Verifica se a extensao do arquivo eh suportada por algum empacotador mapeado
    *
    * @param fileSource arquivo a ser verificado suporte ao empacotamento
    * @return  <code>true</code> - se o arquivo e suportado por algum empacotador<br/>
    * <code>false</code> - se o arquivo nao e suportado nenhum empacotador
    */
   public boolean isCanPack(File fileSource) {
      for (Packer packer : packers.values()) {
         if (packer.supportedTypes().contains(BuildUtil.fileExtension(fileSource).trim().toLowerCase())) {
            return true;
         }
      }
      return false;
   }

   public Compiler getCompiler(File source) {
      return getCompiler(BuildUtil.fileExtension(source));
   }

   public Compiler getCompiler(String fileExtension) {
      if (getCompilers() != null) {
         return getCompilers().get(fileExtension.trim().toLowerCase());
      }
      return null;
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

   public Connection getConnection(final String dbURL, final String user, final String password) throws SQLException {
      Connection dbCon;
      createConnectionsMap();
      dbCon = connections.get(dbURL + ":" + user);
      if (dbCon == null) {
         dbCon = DriverManager.getConnection(dbURL, user, password);
         connections.put(dbURL + ":" + user, dbCon);
      }
      return dbCon;
   }

   private void createConnectionsMap() {
      if (connections == null) {
         connections = new HashMap<String, Connection>();
         Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
               if (connections != null) {
                  for (Connection dbCon : connections.values()) {
                     try {
                        dbCon.close();
                     } catch (SQLException ex) {
                        ex.printStackTrace(System.out);
                     }
                  }
               }
            }
         });
      }
   }

   /**
    * @return the acronym
    */
   public String getAcronym() {
      return acronym;
   }

   /**
    * @param acronym the acronym to set
    */
   public void setAcronym(String acronym) {
      this.acronym = acronym;
   }

   /**
    * @return the systemCode
    */
   public String getSystemCode() {
      return systemCode;
   }

   /**
    * @param systemCode the systemCode to set
    */
   public void setSystemCode(String systemCode) {
      this.systemCode = systemCode;
   }

   public void addHeaderFile(File headerFile) {
      if (headerFiles == null) {
         headerFiles = new ArrayList<File>();
      }
      headerFiles.add(headerFile);
   }

   public void setHeaderFiles(List<File> headerFiles) {
      this.headerFiles = headerFiles;
   }

   public void clearHeaderFiles() {
      if (headerFiles != null) {
         headerFiles.clear();
      }
   }

   public List<File> getHeaderFiles() {
      if (headerFiles != null) {
         return headerFiles;
      }
      return Collections.emptyList();
   }

   public List<File> getTestSourceFiles() {
      return testSourceFiles;
   }

   public void setTestSourceFiles(List<File> sourceFiles) {
      this.testSourceFiles = sourceFiles;
   }

   void setDirectory(String dir) {
      this.directory = dir;
   }

   void setSourceDirectory(String dir) {
      this.sourceDirectory = dir;
   }

   void setTestSourceDirectory(String dir) {
      this.testSourceDirectory = dir;
   }

   void setOutputDirectory(String dir) {
      this.outputDirectory = dir;
   }

   void setTestOutputDirectory(String dir) {
      this.testOutputDirectory = dir;
   }

   /**
    * @return the directory
    */
   public String getDirectory() {
      return directory;
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

   public File getExecutableTestFile() {
      return executableTestFile;
   }

   public void setExecutableTestFile(File executableTestFile) {
      this.executableTestFile = executableTestFile;
   }

   private void configDirectories(final MavenProject project, final Xpp3Dom config) {
      final File baseDir = project.getBasedir();
      final Build build = project.getBuild();
      String dirExtract = valueOf(config, "extractionDirectory");
      setDirectory(configDirectory(baseDir, valueOf(config, "directory"), build.getDirectory()));
      setSourceDirectory(configDirectory(baseDir, valueOf(config, "sourceDirectory"), build.getSourceDirectory()));
      setTestSourceDirectory(configDirectory(baseDir, valueOf(config, "testSourceDirectory"), build.getTestSourceDirectory()));
      setOutputDirectory(configDirectory(baseDir, valueOf(config, "outpurDirectory"), build.getOutputDirectory()));
      setTestOutputDirectory(configDirectory(baseDir, valueOf(config, "testOutputDirectory"), build.getTestOutputDirectory()));
      /* Diretorio onde serao extraidas as dependencias */
      if (dirExtract == null || dirExtract.trim().isEmpty()) {
         setExtractionDirectory(new File(System.getProperty("java.io.tmpdir")));
      } else {
         setExtractionDirectory(new File(dirExtract));
      }
   }

   private void configSources(Xpp3Dom config) {
      setSources(listAnyValuesOf(config, "sources"));
      setExcludeSources(listAnyValuesOf(config, "excludeSources"));
      setIncludeSources(listAnyValuesOf(config, "includeSources"));
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

   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }

   public void setEnvironmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
   }
}
