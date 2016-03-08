/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.vars;

import br.com.uggeri.build.tools.custom.tokenizer.VariableToken;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Fabio
 */
public class MapVariable implements VariableDefinition {
   
   private final Map<String, String> map;

   public MapVariable(Map<String, String> map) {
      this.map = map;
   }

   @Override
   public void appendValue(StringBuilder output, VariableToken varToken) {
      if (map != null && map.size() > 0) {
         if (varToken.getStartText() != null) {
            output.append(varToken.getStartText());
         }
         boolean first = true;
         for (Entry<String, String> entry : map.entrySet()) {
            if (first) {
               first = false;
            } else if (varToken.getElementSeparatorText() != null) {
               output.append(varToken.getElementSeparatorText());
            }
            output.append(entry.getKey());
            if (entry.getValue() != null && ! entry.getValue().isEmpty()) {
               if (varToken.getKeyValueSeparatorText() != null) {
                  output.append(varToken.getKeyValueSeparatorText());
               }
               output.append(entry.getValue());
            }
         }
      }
   }
   
   
}
