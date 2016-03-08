/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.vars;

import br.com.uggeri.build.tools.custom.tokenizer.VariableToken;
import java.util.List;

/**
 *
 * @author Fabio
 */
public class ListVariable implements VariableDefinition {
   
   private final List<String> list;

   public ListVariable(List<String> list) {
      this.list = list;
   }

   @Override
   public void appendValue(StringBuilder output, VariableToken varToken) {
      if (list != null && list.size() > 0) {
         if (varToken.getStartText() != null) {
            output.append(varToken.getStartText());
         }
         boolean first = true;
         for (String s : list) {
            if (first) {
               first = false;
            } else if (varToken.getElementSeparatorText() != null) {
               output.append(varToken.getElementSeparatorText());
            }
            output.append(s);
         }
      }
   }
           
   
}
