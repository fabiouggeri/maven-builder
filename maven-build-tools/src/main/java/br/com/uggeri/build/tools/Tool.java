/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import java.util.Collection;

/**
 *
 * @author ADMIN
 * @param <T>
 * @param <R>
 */
public interface Tool<T extends ExecutionResult, R extends ExecutionRequest> {

   public T execute(R request);

   public Collection<String> supportedTypes();

   public ToolType getToolType();

   public ToolConfig getToolConfig();

   public void setToolConfig(ToolConfig config);
}
