/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.command;

import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 *
 * @author ADMIN
 */
public class DisplayConsumer implements StreamConsumer {

   public void consumeLine(String line) {
      System.out.println(line);
   }
}