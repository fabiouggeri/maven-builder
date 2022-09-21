/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.builder.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author fabio_uggeri
 */
public class SourceFileScanner {

   private final File sourceBaseDir;

   private final List<String> sources;

   private final List<String> sourceIncludes;

   private final List<String> sourceExcludes;

   private SourceFileScannerListener listener = null;

   public SourceFileScanner(File sourceBaseDir, List<String> sources, List<String> sourceIncludes, List<String> sourceExcludes) {
      this.sourceBaseDir = sourceBaseDir;
      this.sources = sources;
      this.sourceIncludes = sourceIncludes;
      this.sourceExcludes = sourceExcludes;
   }

   public SourceFileScanner(File sourceBaseDir) {
      this(sourceBaseDir, null, null, null);
   }

   public List<File> scanFiles() throws SourceFileScanException {
      final List<File> files = new ArrayList<>();

      if (sources == null) {
         files.addAll(listFiles(sourceBaseDir));
      } else {
         for (String source : sources) {
            if (source.indexOf('*') >= 0 || source.indexOf('?') >= 0) {
               Pattern pattern = Pattern.compile(source.replace(".", "\\.").replace('?', '.').replace("*", ".*"));
               if (pattern != null) {
                  files.addAll(listFiles(sourceBaseDir, new SourceFileFilter(pattern)));
               } else {
                  throw new SourceFileScanException("Invalid file filter: " + source);
               }
            } else {
               files.add(new File(sourceBaseDir, source));
            }
         }

      }
      /*
       * Se foi passada uma lista de fontes a incluir, entao acrescenta os
       * fontes informados a lista de fontes.
       */
      includeSources(files);

      /*
       * Se foi passada uma lista de fontes a desconsiderar, entao remove-os
       * da lista de fontes.
       */
      excludeSources(files);

      return files;
   }

   private void addFilesToList(List<File> filesList, File curDir, SourceFileFilter filter, int level) {
      File[] files;
      if (curDir.isDirectory()) {
         if (getListener() != null) {
            getListener().dirScanStarted(curDir);
         }
         if (filter != null) {
            files = curDir.listFiles(filter);
         } else {
            files = curDir.listFiles();
         }
         for (File file : files) {
            if (file.isDirectory()) {
               addFilesToList(filesList, file, filter, level + 1);
            } else {
               filesList.add(file);
               if (getListener() != null) {
                  getListener().fileFound(file);
               }
            }
         }
         if (getListener() != null) {
            getListener().dirScanFinished(curDir);
         }
      }
   }

   private List<File> listFiles(File directory, SourceFileFilter filter) {
      List<File> filesList = new ArrayList<>();
      addFilesToList(filesList, directory, filter, 0);
      return filesList;
   }

   private List<File> listFiles(File directory) {
      return listFiles(directory, null);
   }

   private boolean includeSources(final List<File> filesFound) {
      boolean addFile = false;
      if (sourceIncludes != null) {
         for (String include : sourceIncludes) {
            if (include != null && ! include.isBlank()) {
               final File includeFile = new File(sourceBaseDir, include);
               if (!filesFound.contains(includeFile)) {
                  filesFound.add(includeFile);
                  addFile = true;
                  if (getListener() != null) {
                     getListener().fileIncluded(includeFile);
                  }
               }
            }
         }
      }
      return addFile;
   }

   /**
    * Remove os arquivos informados no parametro IncludeFiles
    */
   private void excludeSources(final List<File> filesFound) {
      if (sourceExcludes != null) {
         List<String> excludes = new LinkedList<>(sourceExcludes);
         for (Iterator<File> itFile = filesFound.iterator(); itFile.hasNext();) {
            File file = itFile.next();
            for (Iterator<String> itExclude = excludes.iterator(); itExclude.hasNext();) {
               final String excludeSource = itExclude.next();
               if (excludeSource != null && ! excludeSource.isBlank()) {
                  String excludeName = excludeSource.replace('/', File.separatorChar).replace('\\', File.separatorChar);
                  if (file.getPath().toLowerCase().endsWith(excludeName)) {
                     itFile.remove();
                     itExclude.remove();
                     if (getListener() != null) {
                        getListener().fileDismissed(file);
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   /**
    * @return the listener
    */
   public SourceFileScannerListener getListener() {
      return listener;
   }

   /**
    * @param listener the listener to set
    */
   public void setListener(SourceFileScannerListener listener) {
      this.listener = listener;
   }

   private class SourceFileFilter implements FilenameFilter {

      private final Pattern pattern;

      public SourceFileFilter(Pattern pattern) {
         this.pattern = pattern;
      }

      @Override
      public boolean accept(File dir, String name) {
         return pattern.matcher(new File(dir, name).getPath()).matches();
      }
   }
}
