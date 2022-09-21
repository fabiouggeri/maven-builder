/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.compiler.project;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.compiler.Compiler;
import org.uggeri.build.tools.log.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 *
 * @author fabio_uggeri
 */
public class ProjectCompilationRequest {
   private final Map<String, String> defines = new HashMap<>();
   private final List<String> includes = new ArrayList<>();
   private final List<File> pendingSources;
   private boolean showFileName;
   private boolean verbose;
   private boolean force;
   private boolean batch;
   private Log log;
   private Properties properties;
   private String outputDir;
   private File sourceDir;
   private int threads = 1;
   private Map<String, Compiler> compilers;
   private Executor executor;
   private Map<String, String> environmentVariables;
   private String pathSeparator = File.separator;

   public ProjectCompilationRequest(List<File> sources) {
      this.pendingSources = new ArrayList<>(sources);
   }

   /**
    * @return the defines
    */
   public Map<String, String> getDefines() {
      return defines;
   }

   /**
    * @return the includes
    */
   public List<String> getIncludes() {
      return includes;
   }

   /**
    * @return the pendingSources
    */
   public List<File> getPendingSources() {
      return pendingSources;
   }

   /**
    * @return the showFileName
    */
   public boolean isShowFileName() {
      return showFileName;
   }

   /**
    * @param showFileName the showFileName to set
    */
   public void setShowFileName(boolean showFileName) {
      this.showFileName = showFileName;
   }

   /**
    * @return the verbose
    */
   public boolean isVerbose() {
      return verbose;
   }

   /**
    * @param verbose the verbose to set
    */
   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
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
    * @return the log
    */
   public Log getLog() {
      return log;
   }

   /**
    * @param log the log to set
    */
   public void setLog(Log log) {
      this.log = log;
   }

   /**
    * @return the properties
    */
   public Properties getProperties() {
      return properties;
   }

   /**
    * @param properties the properties to set
    */
   public void setProperties(Properties properties) {
      this.properties = properties;
   }

   /**
    * @return the outputDir
    */
   public String getOutputDir() {
      return outputDir;
   }

   /**
    * @param outputDir the outputDir to set
    */
   public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
   }

   /**
    * @return the sourceDir
    */
   public File getSourceDir() {
      return sourceDir;
   }

   /**
    * @param sourceDir the sourceDir to set
    */
   public void setSourceDir(File sourceDir) {
      this.sourceDir = sourceDir;
   }

   public int getThreads() {
      return threads;
   }

   public void setThreads(int threads) {
      this.threads = threads;
   }

   public Map<String, Compiler> getCompilers() {
      if (compilers == null) {
         return Collections.emptyMap();
      }
      return compilers;
   }

   public void setCompilers(Map<String, Compiler> compilers) {
      this.compilers = compilers;
   }

   public Compiler getCompiler(String fileExtension) {
      if (getCompilers() != null) {
         return getCompilers().get(fileExtension.trim().toLowerCase());
      }
      return null;
   }

   public Compiler getCompiler(File source) {
      return getCompiler(BuildUtil.fileExtension(source));
   }

   public Executor getExecutor() {
      return executor;
   }

   public void setExecutor(Executor executor) {
      this.executor = executor;
   }

   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }

   public void setEnvironmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
   }

   public String getPathSeparator() {
      return pathSeparator;
   }

   public void setPathSeparator(String pathSeparator) {
      this.pathSeparator = pathSeparator;
   }
}
