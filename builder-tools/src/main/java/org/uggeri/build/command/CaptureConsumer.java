/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 *
 * @author ADMIN
 */
public class CaptureConsumer implements StreamConsumer {

   private final Collection<String> lines = new ArrayList<>();

   @Override
   public void consumeLine(String line) {
      if (line != null) {
         lines.add(line);
      }
   }

   public void clear() {
      lines.clear();
   }

   public Collection<String> getLines() {
      return Collections.unmodifiableCollection(lines);
   }
}