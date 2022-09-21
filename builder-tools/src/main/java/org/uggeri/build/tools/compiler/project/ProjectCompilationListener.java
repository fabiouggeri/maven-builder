/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools.compiler.project;

import org.uggeri.build.tools.compiler.Compiler;
import org.uggeri.build.tools.compiler.CompilationRequest;
import org.uggeri.build.tools.compiler.CompilationResult;
import java.io.File;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public interface ProjectCompilationListener {

   public void compilationStarting(Compiler compiler, List<File> sources);

   public void compilationSucessful(Compiler compiler, CompilationRequest request, CompilationResult result);

   public void compilationFailed(Compiler compiler, CompilationRequest request, CompilationResult result);

   public void preCompilationValidationFailed(Compiler compiler, CompilationRequest request);

   public void posCompilationValidationFailed(Compiler compiler, CompilationRequest request);

   public void resultObjectsScheduled(Compiler compiler, CompilationRequest request, CompilationResult result);

   public void compilationFinished(Compiler compiler);

}
