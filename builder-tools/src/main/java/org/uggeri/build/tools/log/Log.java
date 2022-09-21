/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.log;

/**
 *
 * @author fabio_uggeri
 */
public interface Log {

   public void info(String log);
   public void debug(String log);
   public void warn(String log);
   public void error(String log);
   public void info(String log, Exception ex);
   public void debug(String log, Exception ex);
   public void warn(String log, Exception ex);
   public void error(String log, Exception ex);
   public boolean isDebugEnabled();
}
