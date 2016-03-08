/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import br.com.uggeri.build.tools.Tool;
import java.io.File;
import java.util.Collection;

/**
 *
 * @author ADMIN
 */
public interface Compiler extends Tool<CompilationResult, CompilationRequest> {

   public boolean isBatchSupport();
   
   public String getDefaultOutputExtension();

   public String getDefaultOutputExtension(String inputExtension);

   public Collection<String> supportedIncludes();

   public File getOutputFile(File sourceFile, CompilationRequest request);

   public boolean preCompilation(File srcFile, CompilationRequest request);

   public boolean posCompilation(File srcFile, File outFile, CompilationRequest request);
}
