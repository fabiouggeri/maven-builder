/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.custom.vars;

import org.uggeri.build.tools.custom.tokenizer.VariableToken;

/**
 *
 * @author Fabio
 */
public interface VariableDefinition {
   
   public void appendValue(StringBuilder output, VariableToken varToken);
}
