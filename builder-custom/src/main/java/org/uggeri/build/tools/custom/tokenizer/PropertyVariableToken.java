/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.custom.tokenizer;

/**
 *
 * @author Fabio
 */
public class PropertyVariableToken extends VariableToken {
   
   private String valueTest;
   
   private boolean appendPropertyValue;
   
   private boolean testEqual;
   
   private boolean testNotDefined;

   public PropertyVariableToken(String variableName, String startText, String elementSeparatorText, String keyValueSeparatorText, String valueTest, boolean appendPropertyValue, boolean testEqual, boolean testNotDefined) {
      super(variableName, startText, elementSeparatorText, keyValueSeparatorText);
      this.valueTest = valueTest;
      this.appendPropertyValue = appendPropertyValue;
      this.testEqual = testEqual;
      this.testNotDefined = testNotDefined;
   }

   public PropertyVariableToken(String variableName) {
      this(variableName, "", "", "", "", false, true, false);
   }
   

   /**
    * @return the valueTest
    */
   public String getValueTest() {
      return valueTest;
   }

   /**
    * @param valueTest the valueTest to set
    */
   public void setValueTest(String valueTest) {
      this.valueTest = valueTest;
   }

   public boolean isAppendPropertyValue() {
      return appendPropertyValue;
   }

   public void setAppendPropertyValue(boolean appendPropertyValue) {
      this.appendPropertyValue = appendPropertyValue;
   }

   public boolean isTestEqual() {
      return testEqual;
   }

   public void setTestEqual(boolean testEqual) {
      this.testEqual = testEqual;
   }

   public boolean isTestNotDefined() {
      return testNotDefined;
   }

   public void setTestNotDefined(boolean testNotDefined) {
      this.testNotDefined = testNotDefined;
   }
   
   @Override
   public String toString() {
      return "#{" + testType() + getVariableName() + testOperation() + "," + getStartText() + "," + getElementSeparatorText() + "," + getKeyValueSeparatorText() + "}";
   }

   private String testOperation() {
      if (valueTest != null && ! valueTest.isEmpty()) {
         if (testEqual) {
            return "=" + valueTest;
         } else {
            return "!=" + valueTest;
         }
      } else {
         return "";
      }
   }

   private String testType() {
      if (appendPropertyValue) {
         return "&";
      } else if (testNotDefined) {
         return "!";
      } else {
         return "?";
      }
   }
}
