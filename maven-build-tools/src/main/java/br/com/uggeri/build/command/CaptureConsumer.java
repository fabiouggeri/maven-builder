/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 *
 * @author ADMIN
 */
public class CaptureConsumer implements StreamConsumer {

   private final Collection<String> lines = new ArrayList<String>();

   @Override
   public void consumeLine(String line) {
      lines.add(line);
   }

   public void clear() {
      lines.clear();
   }

   public Collection<String> getLines() {
      return Collections.unmodifiableCollection(lines);
   }
}