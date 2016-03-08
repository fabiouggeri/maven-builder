/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import java.io.File;
import java.util.Collection;

/**
 *
 * @author fabio_uggeri
 * @param <T>
 * @param <R>
 */
public interface ExternalTool<T extends ExecutionResult, R extends ExecutionRequest> extends Tool<T, R> {

   public File getExecutable();

   public void setExecutable(File executable);

   public void setExecutionDependencies(Collection<File> files);

   public Collection<File> getExecutionDependencies();
}
