/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.custom;

import org.uggeri.build.tools.BuildUtil;
import org.uggeri.build.tools.ExecutionRequest;
import org.uggeri.build.tools.ExternalTool;
import org.uggeri.build.tools.custom.compiler.CustomCompilationException;
import org.uggeri.build.tools.custom.tokenizer.CommandLineToken;
import org.uggeri.build.tools.custom.tokenizer.CommandLineTokenizer;
import org.uggeri.build.tools.custom.tokenizer.CommandLineTokenizerException;
import org.uggeri.build.tools.custom.tokenizer.PropertyVariableToken;
import org.uggeri.build.tools.custom.tokenizer.VariableToken;
import org.uggeri.build.tools.custom.vars.StringVariable;
import org.uggeri.build.tools.custom.vars.VariableDefinition;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @author Fabio 
 * #{?teste} -> se teste esta definido 
 * #{?teste='x'} -> se teste esta definido e igual a 'x' 
 * #{?teste!='x'} -> se teste esta definido e diferente de 'x' 
 * #{&teste} -> se teste esta definido (inclui seu conteudo na saida) 
 * #{&teste='x'} -> se teste esta definido e igual a 'x' (inclui seu conteudo na saida) 
 * #{&teste!='x'} -> se teste esta definido e diferente de 'x' (inclui seu conteudo na saida) 
 * #{!teste} -> se teste nao esta definido
 */
public class CustomCommandLineBuilder {

   private final Map<String, VariableDefinition> variables = new HashMap<String, VariableDefinition>();

   public CustomCommandLineBuilder() {
   }

   public VariableDefinition getVariable(final String varName) {
      return variables.get(varName.toLowerCase());
   }

   public void putVariable(final String varName, VariableDefinition var) {
      variables.put(varName.toLowerCase(), var);
   }

   public String buildCommandLine(final String commandLine, ExternalTool tool, ExecutionRequest request) {
      try {
         final StringBuilder output = new StringBuilder();
         CommandLineTokenizer st = new CommandLineTokenizer();
         List<CommandLineToken> tokens = st.parse(commandLine);
         for (CommandLineToken t : tokens) {
            if (t instanceof PropertyVariableToken) {
               final PropertyVariableToken varToken = (PropertyVariableToken) t;
               /* Se foi passada por linha de comando uma propriedade com este nome, entao o value sera != null */
               if (request.containsOption(varToken.getVariableName())) {
                  /* Nao eh um teste de definicao da variavel */
                  if (!varToken.isTestNotDefined()) {
                     String value = request.getOption(((PropertyVariableToken) t).getVariableName());
                     /* Verifica a variavel tem um teste definido para inclusao */
                     if (varToken.getValueTest() != null && !varToken.getValueTest().isEmpty()) {
                        /*Caso o teste seja verdadeiro inclui a variavel na linha de comando */
                        if (varToken.isTestEqual()) {
                           if (value.equalsIgnoreCase(varToken.getValueTest())) {
                              new StringVariable(varToken.isAppendPropertyValue() ? value : "").appendValue(output, varToken);
                           }
                        } else {
                           if (!value.equalsIgnoreCase(varToken.getValueTest())) {
                              new StringVariable(varToken.isAppendPropertyValue() ? value : "").appendValue(output, varToken);
                           }
                        }
                        /* Se a varivel nao inclui um teste, entao inclui ela na linha de comando independente de qualquer teste */
                     } else {
                        new StringVariable(varToken.isAppendPropertyValue() ? value : "").appendValue(output, varToken);
                     }
                  }
               } else if (varToken.isTestNotDefined()) {
                  new StringVariable("").appendValue(output, varToken);
               }
               /* Se a variavel nao eh baseada nas propriedades do Maven, entao ela deve ser uma previamente definida pela classe */
            } else if (t instanceof VariableToken) {
               final VariableDefinition var = getVariable(((VariableToken) t).getVariableName());
               if (var != null) {
                  var.appendValue(output, (VariableToken) t);
               } else {
                  throw new CustomCompilationException("Unknown variable found '" + ((VariableToken) t).getVariableName() + "'");
               }
               /* Se nao for uma variavel, entao simplesmente adiciona a linha de comando */
            } else {
               output.append(t.toString());
            }
         }
         if (tool.getExecutable() == null) {
            addPathToExecutable(output, request);
         }
         return output.toString();
      } catch (CommandLineTokenizerException ex) {
         throw new CustomCompilationException("Error parsing command line: " + commandLine, ex);
      }
   }

   private void addPathToExecutable(StringBuilder cmd, ExecutionRequest request) {
      if (request.getEnvironmentVariables() != null) {
         int exeEndIndex = executableEndIndex(cmd);
         String exeName = cmd.substring(0, exeEndIndex).trim();
         if (FileUtils.dirname(exeName).isEmpty()) {
            if (FileUtils.getExtension(exeName).isEmpty() && BuildUtil.isWindows()) {
               exeName += ".exe";
            }
            for (Entry<String, String> var : request.getEnvironmentVariables().entrySet()) {
               if (var.getKey().equalsIgnoreCase("PATH")) {
                  String[] paths = var.getValue().split(File.pathSeparator);
                  for (String path : paths) {
                     final File file = new File(path, exeName);
                     if (file.isFile()) {
                        cmd.replace(0, exeEndIndex, BuildUtil.canonicalPathName(file));
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private int executableEndIndex(StringBuilder cmd) {
      int index = 0;
      boolean inString = false;
      while (index < cmd.length() && Character.isSpaceChar(cmd.charAt(index))) {
         ++index;
      }
      while (index < cmd.length()) {
         final char c = cmd.charAt(index);
         if (inString) {
            if (c == '"') {
               inString = false;
            }
         } else if (Character.isSpaceChar(c)) {
            return index;
         } else if (c == '"') {
            inString = true;
         }
         ++index;
      }
      return index;
   }
}
