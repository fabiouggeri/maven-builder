/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import java.util.Collection;

/**
 *
 * @author fabio_uggeri
 */
public interface ExecutionResult {

   public Collection<String> getOutput();
   
   public int getExitCode();
   
   public boolean isSuccessful();
}
