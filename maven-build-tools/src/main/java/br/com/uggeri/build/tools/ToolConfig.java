/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.tools;

/**
 *
 * @author fabio_uggeri
 */
public class ToolConfig {

   private String outputType = "";

   private String[] sourceTypes = null;

   private String[] includeTypes = null;

   private String[] packagingTypes = null;

   private String commandLine = "";

   private boolean batchSupport = false;

   private String startOptions = null;

   private String endOptions = null;

   /**
    * @return the outputType
    */
   public String getOutputType() {
      return outputType;
   }

   /**
    * @param outputType the outputType to set
    */
   public void setOutputType(String outputType) {
      this.outputType = outputType;
   }

   /**
    * @return the sourceTypes
    */
   public String[] getSourceTypes() {
      return sourceTypes;
   }

   /**
    * @param sourceTypes the sourceTypes to set
    */
   public void setSourceTypes(String[] sourceTypes) {
      this.sourceTypes = sourceTypes;
   }

   /**
    * @return the includeTypes
    */
   public String[] getIncludeTypes() {
      return includeTypes;
   }

   /**
    * @param includeTypes the includeTypes to set
    */
   public void setIncludeTypes(String[] includeTypes) {
      this.includeTypes = includeTypes;
   }

   /**
    * @return the packagingTypes
    */
   public String[] getPackagingTypes() {
      return packagingTypes;
   }

   /**
    * @param packagingTypes the packagingTypes to set
    */
   public void setPackagingTypes(String[] packagingTypes) {
      this.packagingTypes = packagingTypes;
   }

   /**
    * @return the commandLine
    */
   public String getCommandLine() {
      return commandLine;
   }

   /**
    * @param commandLine the commandLine to set
    */
   public void setCommandLine(String commandLine) {
      this.commandLine = commandLine;
   }

   /**
    * @return the batchSupport
    */
   public boolean isBatchSupport() {
      return batchSupport;
   }

   /**
    * @param batchSupport the batchSupport to set
    */
   public void setBatchSupport(boolean batchSupport) {
      this.batchSupport = batchSupport;
   }

   public String getEndOptions() {
      return endOptions;
   }

   public void setEndOptions(String endOptions) {
      this.endOptions = endOptions;
   }

   public String getStartOptions() {
      return startOptions;
   }

   public void setStartOptions(String startOptions) {
      this.startOptions = startOptions;
   }
}
