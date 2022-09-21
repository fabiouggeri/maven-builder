/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.maven.builder;

import org.uggeri.build.tools.log.Log;

/**
 *
 * @author fabio_uggeri
 */
public class MavenLogWrapper implements Log {

   private final org.apache.maven.plugin.logging.Log mavenLog;

   public MavenLogWrapper(org.apache.maven.plugin.logging.Log mavenLog) {
      this.mavenLog = mavenLog;
   }

   @Override
   public void info(String log) {
      mavenLog.info(log);
   }

   @Override
   public void debug(String log) {
      mavenLog.debug(log);
   }

   @Override
   public void warn(String log) {
      mavenLog.warn(log);
   }

   @Override
   public void error(String log) {
      mavenLog.error(log);
   }

   @Override
   public void info(String log, Exception ex) {
      mavenLog.info(log, ex);
   }

   @Override
   public void debug(String log, Exception ex) {
      mavenLog.debug(log, ex);
   }

   @Override
   public void warn(String log, Exception ex) {
      mavenLog.warn(log, ex);
   }

   @Override
   public void error(String log, Exception ex) {
      mavenLog.error(log, ex);
   }

   @Override
   public boolean isDebugEnabled() {
      return mavenLog.isDebugEnabled();
   }

}
