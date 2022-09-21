/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.helper.mojo;

import org.uggeri.maven.helper.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.uggeri.build.tools.BuildUtil;
import org.uggeri.maven.builder.MavenLogWrapper;

/**
 *
 * @author fabio_uggeri
 */
@Mojo(name = "zip", requiresProject = false, requiresDirectInvocation = true)
public class ZipMojo extends AbstractMojo {

   @Parameter(property = "source", required = false, defaultValue = ".")
   private File source = null;
   
   @Parameter(property = "zipName", required = true)
   private File zipName = null;
   
   @Parameter(property = "recursive", required = false, defaultValue = "true")
   private boolean recursive = true;
   
   @Parameter(property = "filterExtension", required = false)
   private String filterExtension = null;
   
   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      final List<File> files = new ArrayList<File>();
      final String extensions[] = filterExtension != null ? filterExtension.toLowerCase().split(",") : new String[] {};
      final String error;

      if (source == null) {
         throw new MojoExecutionException("Source was not informed");
      }
      if (zipName == null) {
         throw new MojoExecutionException("Zip name was not informed");
      }
      getLog().info("Procurando arquivos...");
      if (source.isFile()) {
         files.add(source);
         source = source.getParentFile();
      } else if (source.isDirectory()) {
         Arrays.sort(extensions);
         scanFiles(files, source, recursive, extensions);
      } else {
         throw new MojoExecutionException("Source is not a file or directory");
      }
      if (zipName.getName().indexOf('.') < 0) {
         zipName = new File(zipName.getParent(), zipName.getName() + ".zip");
      }
      getLog().info("Creating zip " + zipName);
      error = BuildUtil.zip(files, zipName, source, new MavenLogWrapper(getLog()), false);
      if (error != null) {
         throw new MojoExecutionException(error);
      }
   }

   private void scanFiles(final List<File> files, final File dir, final boolean recursive, final String[] extensions) {
      getLog().info("." + File.separator + FileUtil.getRelativePath(source, dir));
      for (File f : dir.listFiles()) {
         if(f.isDirectory()) {
            if (recursive) {
               scanFiles(files, f, recursive, extensions);
            }
         } else if (f.isFile()) {
            if (extensions.length == 0 || Arrays.binarySearch(extensions, FileUtils.extension(f.getName().toLowerCase())) >= 0) {
               files.add(f);
               getLog().debug(FileUtil.getRelativePath(source, f));
            }
         }
      }
   }
   
}
