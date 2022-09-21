/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.custom.vars;

import org.uggeri.build.tools.custom.tokenizer.VariableToken;
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
         for (int i = 0; i < list.size(); i++) {
            final String s = list.get(i);
            varToken.appendToText(output, s, i == 0, i == list.size() - 1);
         }
      }
   }
}
