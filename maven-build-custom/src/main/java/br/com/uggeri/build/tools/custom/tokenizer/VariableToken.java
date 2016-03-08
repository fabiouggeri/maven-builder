/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.tokenizer;

/**
 *
 * @author Fabio
 */
public class VariableToken implements CommandLineToken {
   
   private String variableName;
   
   private String startText;
   
   private String elementSeparatorText;
   
   private String keyValueSeparatorText;

   public VariableToken(final String variableName, final String startText, final String elementSeparatorText, final String keyValueSeparatorText) {
      this.variableName = variableName;
      this.startText = startText;
      this.elementSeparatorText = elementSeparatorText;
      this.keyValueSeparatorText = keyValueSeparatorText;
   }

   public VariableToken(final String variableName) {
      this(variableName, "", "", "");
   }

   
   /**
    * @return the variableName
    */
   public String getVariableName() {
      return variableName;
   }

   /**
    * @param variableName the variableName to set
    */
   public void setVariableName(String variableName) {
      this.variableName = variableName;
   }

   /**
    * @return the startText
    */
   public String getStartText() {
      return startText;
   }

   /**
    * @param startText the startText to set
    */
   public void setStartText(String startText) {
      this.startText = startText;
   }

   /**
    * @return the elementSeparatorText
    */
   public String getElementSeparatorText() {
      return elementSeparatorText;
   }

   /**
    * @param elementSeparatorText the elementSeparatorText to set
    */
   public void setElementSeparatorText(String elementSeparatorText) {
      this.elementSeparatorText = elementSeparatorText;
   }

   /**
    * @return the keyValueSeparatorText
    */
   public String getKeyValueSeparatorText() {
      return keyValueSeparatorText;
   }

   /**
    * @param keyValueSeparatorText the keyValueSeparatorText to set
    */
   public void setKeyValueSeparatorText(String keyValueSeparatorText) {
      this.keyValueSeparatorText = keyValueSeparatorText;
   }

   @Override
   public String toString() {
      return "#{" + variableName + "," + startText + "," + elementSeparatorText + "," + keyValueSeparatorText + "}";
   }
}
