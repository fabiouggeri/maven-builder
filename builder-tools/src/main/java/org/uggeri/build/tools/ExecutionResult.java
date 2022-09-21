/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools;

import java.util.Collection;

/**
 *
 * @author fabio_uggeri
 */
public interface ExecutionResult {

   Collection<String> getOutput();
   
   int getExitCode();
   
   boolean isSuccessful();
}
