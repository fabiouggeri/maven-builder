/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.compiler;

import org.uggeri.build.tools.ExecutionRequest;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fabio_uggeri
 */
public interface CompilationRequest extends ExecutionRequest {
   
   boolean isBatch();
   
   boolean isForce();

   Map<String, String> getDefines();

   void setDefines(Map<String, String> defines);
   
   List<String> getIncludesPaths();

   void setIncludesPaths(List<String> paths);
   
   File getSourceDirectory();
   
   void setSourceDirectory(File file);
}
