/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import br.com.uggeri.build.tools.ExecutionRequest;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fabio_uggeri
 */
public interface CompilationRequest extends ExecutionRequest {
   
   public boolean isBatch();
   
   public boolean isForce();

   public Map<String, String> getDefines();

   public void setDefines(Map<String, String> defines);
   
   public List<String> getIncludesPaths();

   public void setIncludesPaths(List<String> paths);
   
   public File getSourceDirectory();
   
   public void setSourceDirectory(File file);
}
