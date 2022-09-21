/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.custom.vars;

import org.uggeri.build.tools.custom.tokenizer.VariableToken;
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
         boolean first = true;
         for (Entry<String, String> entry : map.entrySet()) {
            varToken.appendToText(output, entry, first);
            first = false;
         }
      }
   }
   
   
}
