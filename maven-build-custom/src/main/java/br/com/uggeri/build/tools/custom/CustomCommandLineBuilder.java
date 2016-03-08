/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom;

import br.com.uggeri.build.tools.ExecutionRequest;
import br.com.uggeri.build.tools.custom.compiler.CustomCompilationException;
import br.com.uggeri.build.tools.custom.tokenizer.CommandLineToken;
import br.com.uggeri.build.tools.custom.tokenizer.CommandLineTokenizer;
import br.com.uggeri.build.tools.custom.tokenizer.CommandLineTokenizerException;
import br.com.uggeri.build.tools.custom.tokenizer.PropertyVariableToken;
import br.com.uggeri.build.tools.custom.tokenizer.VariableToken;
import br.com.uggeri.build.tools.custom.vars.StringVariable;
import br.com.uggeri.build.tools.custom.vars.VariableDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

   public String buildCommandLine(final String commandLine, ExecutionRequest request) {
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
                  if (! varToken.isTestNotDefined()) {
                     String value = request.getOption(((PropertyVariableToken) t).getVariableName());
                     /* Verifica a variavel tem um teste definido para inclusao */
                     if (varToken.getValueTest() != null && ! varToken.getValueTest().isEmpty()) {
                        /*Caso o teste seja verdadeiro inclui a variavel na linha de comando */
                        if (varToken.isTestEqual()) {
                           if (value.equalsIgnoreCase(varToken.getValueTest())) {
                              new StringVariable(varToken.isAppendPropertyValue() ? value : "").appendValue(output, varToken);
                           }
                        } else {
                           if (! value.equalsIgnoreCase(varToken.getValueTest())) {
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
                  throw new CustomCompilationException("Unknown variable found '" + ((VariableToken)t).getVariableName() + "'");
               }
            /* Se nao for uma variavel, entao simplesmente adiciona a linha de comando */   
            } else {
               output.append(t.toString());
            }
         }
         return output.toString();
      } catch (CommandLineTokenizerException ex) {
         throw new CustomCompilationException("Error parsing command line: " + commandLine, ex);
      }
   }
}
