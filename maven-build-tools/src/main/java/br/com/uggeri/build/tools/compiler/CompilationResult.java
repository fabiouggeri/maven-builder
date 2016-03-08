/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.compiler;

import br.com.uggeri.build.tools.ExecutionResult;
import java.io.File;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public interface CompilationResult extends ExecutionResult {

   public List<File> getOutputFiles();
}
