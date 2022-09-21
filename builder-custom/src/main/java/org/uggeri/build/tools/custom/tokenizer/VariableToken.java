/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.custom.tokenizer;

import java.io.File;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @author Fabio
 */
public class VariableToken implements CommandLineToken {

   final private static Pattern FUN_PATTERN = Pattern.compile("\\$\\(\\s*([a-z]+)\\s*(,\\s*([0-9]+|\"[^\"}]+\")\\s*(,\\s*([0-9]+|\"[^\"}]+\"))?\\s*)?\\)");
   
   final private static Pattern NUM_PATTERN = Pattern.compile("[0-9]+");
   
   final private static Pattern STR_PATTERN = Pattern.compile("\"[^\"]*\"");

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

   private boolean evalFunctions(StringBuilder output, String text, String value) {
      final Matcher m = FUN_PATTERN.matcher(text);
      boolean change = false;
      int start = 0;
      while (m.find(start)) {
         if (m.groupCount() > 0) {
            final String name = m.group(1);
            String replaceText = null;
            if ("name".equals(name)) {
               final File file = new File(value);
               replaceText = FileUtils.removeExtension(file.getName());
            } else if ("path".equals(name)) {
               final File file = new File(value);
               replaceText = file.getParent();
               if (! replaceText.isEmpty()) {
                  char c = replaceText.charAt(replaceText.length() - 1);
                  if (c == '\\' || c == '/') {
                     replaceText = replaceText.substring(0, replaceText.length() - 1);
                  }
               }
            } else if ("extension".equals(name)) {
               final File file = new File(value);
               int pos = file.getName().lastIndexOf('.');
               if (pos >= 0) {
                  replaceText = file.getName().substring(pos);
               }
            }
            if (replaceText != null) {
               String par1 = null;
               String par2 = null;
               if (m.groupCount() > 2) {
                  par1 = m.group(3);
                  if (m.groupCount() > 4) {
                     par2 = m.group(5);
                  }
               }
               output.append(text.substring(start, m.start()));
               if (par1 != null) {
                  if (par2 != null) {
                     if (NUM_PATTERN.matcher(par1).matches()) {
                        if (NUM_PATTERN.matcher(par2).matches()) {
                           replaceText = replaceText.substring(Integer.parseInt(par1), Integer.parseInt(par2));
                        } else if (STR_PATTERN.matcher(par2).matches()) {
                           String subs = par2.substring(1, par2.length() - 1);
                           replaceText = replaceText.substring(0, Integer.parseInt(par1)) + subs + replaceText.substring(Integer.parseInt(par1));
                        } else {
                           replaceText = replaceText.substring(Integer.parseInt(par1));
                        }
                     } else if (STR_PATTERN.matcher(par1).matches()) {
                        String subs1 = par1.substring(1, par1.length() - 1);
                        if (STR_PATTERN.matcher(par2).matches()) {
                           String subs2 = par2.substring(1, par2.length() - 1);
                           replaceText = replaceText.replace(subs1, subs2);
                        } else if (NUM_PATTERN.matcher(par2).matches()) {
                           replaceText = subs1 + replaceText.substring(Integer.parseInt(par2));
                        } else {
                           if (replaceText.startsWith(subs1)) {
                              replaceText = replaceText.substring(subs1.length());
                           }
                        }
                     }
                  } else {
                     if (NUM_PATTERN.matcher(par1).matches()) {
                        replaceText = replaceText.substring(Integer.parseInt(par1));
                     } else if (STR_PATTERN.matcher(par1).matches()) {
                        String subs = par1.substring(1, par1.length() - 1);
                        if (replaceText.startsWith(subs)) {
                           replaceText = replaceText.substring(subs.length());
                        }
                     }
                  }
               }
               output.append(replaceText);
               change = true;
            }
         }
         start = m.end();
      }
      if (change && start < text.length()) {
         output.append(text.substring(start));
      }
      return change;
   }

//   public static void main(String[] args) {
//      StringBuilder sb = new StringBuilder();
//      evalFunctions(sb, "-Wl,-L $(path, \"\\\", \"/\") -l$(name) -C", "c:\\temp\\subdir\\teste.txt");
//   }

   public void appendToText(StringBuilder output, String value, boolean first, boolean last) {
      boolean replaced = false;
      if (first) {
         if (startText != null) {
            replaced = evalFunctions(output, startText, value);
         }
      } else if (elementSeparatorText != null) {
         replaced = evalFunctions(output, elementSeparatorText, value);
      }
      if (last && keyValueSeparatorText != null) {
         replaced = evalFunctions(output, keyValueSeparatorText, value) || replaced;
      }
      if (!replaced) {
         if (first) {
            if (startText != null) {
               output.append(startText);
            }
         } else if (elementSeparatorText != null) {
            output.append(elementSeparatorText);
         }
         output.append(value);
         if (last && keyValueSeparatorText != null) {
            output.append(keyValueSeparatorText);
         }
      }
   }

   public void appendToText(StringBuilder output, Entry<?, ?> value, boolean first) {
      if (first) {
         if (startText != null) {
            output.append(startText);
         }
      } else if (elementSeparatorText != null) {
         output.append(elementSeparatorText);
      }
      output.append(value.getKey().toString());
      if (value.getValue() != null && !value.getValue().toString().isEmpty()) {
         if (keyValueSeparatorText != null) {
            output.append(keyValueSeparatorText);
         }
         output.append(value.getValue());
      }
   }
}
