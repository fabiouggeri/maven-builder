/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.command;

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