/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.helper.mojo;

import org.uggeri.maven.helper.FileUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.uggeri.build.tools.BuildUtil;

/**
 *
 * @author fabio_uggeri
 */
@Mojo(name = "multizip", requiresProject = false, requiresDirectInvocation = true)
public class MultiZipMojo extends AbstractMojo {

   @Parameter(required = true)
   private File zipName = null;

   @Parameter(required = true)
   private List<ZipSource> sources = null;

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      if (sources == null) {
         throw new MojoExecutionException("Sources was not informed");
      }
      if (zipName == null) {
         throw new MojoExecutionException("Zip name was not informed");
      }
      if (zipName.getName().indexOf('.') < 0) {
         zipName = new File(zipName.getParent(), zipName.getName() + ".zip");
      }
      if (zipName.isFile()) {
         if (!zipName.delete()) {
            throw new MojoExecutionException("Could not remove file " + zipName);
         }
      }
      zipWithApacheCommons();
   }

   private Path targetPath(Map<Path, Path> paths, Path pathSearching) {
      for (Entry<Path, Path> entry : paths.entrySet()) {
         if (entry.getKey().equals(pathSearching)) {
            return entry.getValue();
         }
      }
      return null;
   }

   private void scanFiles(final List<File> files, final File dir, final boolean recursive, final String[] extensions) {
      getLog().info(dir.getPath());
      for (File f : dir.listFiles()) {
         if (f.isDirectory()) {
            if (recursive) {
               scanFiles(files, f, recursive, extensions);
            }
         } else if (f.isFile()) {
            if (extensions.length == 0 || Arrays.binarySearch(extensions, FileUtils.extension(f.getName().toLowerCase())) >= 0) {
               files.add(f);
               getLog().debug(f.getPath());
            }
         }
      }
   }

   private String pathNameInZip(final String pathInZip, final File sourcePathName, final File file) {
      if (sourcePathName.isDirectory()) {
         if (pathInZip.isEmpty()) {
            return FileUtil.getRelativePath(sourcePathName, file);
         } else {
            return pathInZip + File.separator + FileUtil.getRelativePath(sourcePathName, file);
         }
      } else {
         return pathInZip + File.separator + file.getName();
      }
   }

   private void zipWithApacheCommons() throws MojoExecutionException {
      try (ZipArchiveOutputStream zipOutputStream = createZip()) {
         final Map<Path, Path> mapLinks = new HashMap<>();
         final Map<Path, Path> mapFiles = new HashMap<>();

         for (ZipSource zipSource : sources) {
            final String extensions[] = zipSource.getFilterExtensions() != null ? zipSource.getFilterExtensions().toLowerCase().split(",") : new String[]{};
            final List<File> files = new ArrayList<>();

            zipSource.setPathInZip(BuildUtil.platformPath(zipSource.getPathInZip().trim()));
            if (zipSource.getPathInZip().endsWith("\\") || zipSource.getPathInZip().endsWith("/")) {
               zipSource.setPathInZip(zipSource.getPathInZip().substring(0, zipSource.getPathInZip().length() - 1));
            }
            Arrays.sort(extensions);
            if (zipSource.getSourcePathName().isDirectory()) {
               scanFiles(files, zipSource.getSourcePathName(), zipSource.isRecursive(), extensions);
            } else if (zipSource.getSourcePathName().isFile()) {
               files.add(zipSource.getSourcePathName());
            } else {
               throw new MojoExecutionException(zipSource.getSourcePathName() + " is not a directory or a file.");
            }
            for (File f : files) {
               final String pathNameInZip = pathNameInZip(zipSource.getPathInZip(), zipSource.getSourcePathName(), f);
               zipFile(zipOutputStream, f, pathNameInZip, mapLinks, mapFiles);
            }
         }
         for (Entry<Path, Path> entry : mapLinks.entrySet()) {
            final Path linkTarget = targetPath(mapFiles, entry.getValue());
            if (linkTarget != null) {
               zipFile(zipOutputStream, linkTarget.toFile(), entry.getValue().toString(), null, null);
            }
         }
      } catch (IOException ex) {
         throw new MojoExecutionException("Error creating zip " + zipName + ".", ex);
      }
   }

   private ZipArchiveOutputStream createZip() throws MojoExecutionException {
      ZipArchiveOutputStream zipOutputStream = null;
      try {
         if (zipName.createNewFile()) {
            try {
               zipOutputStream = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(zipName)));
            } catch (FileNotFoundException ex) {
               throw new MojoExecutionException("Error to create" + zipName + ".", ex);
            }
         }
      } catch (IOException ex) {
         throw new MojoExecutionException("Error to create " + zipName + ".", ex);
      }
      return zipOutputStream;
   }

   private void zipFile(ZipArchiveOutputStream zipOutputStream, File file, String pathNameInZip, final Map<Path, Path> mapLinks,
           final Map<Path, Path> mapFiles) throws MojoExecutionException {
      final ZipArchiveEntry zipEntry = new ZipArchiveEntry(pathNameInZip);
      try {
         getLog().debug("Compressing " + file.getName() + "...");
         if (BuildUtil.isLinux()) {
            final Path sourcePath = Paths.get(file.toURI());
            zipEntry.setUnixMode(BuildUtil.unixMode(Files.getPosixFilePermissions(sourcePath)));
            if (mapLinks != null && mapFiles != null) {
               if (Files.isSymbolicLink(sourcePath)) {
                  mapLinks.put(sourcePath, sourcePath.toRealPath().toAbsolutePath());
               } else {
                  mapFiles.put(sourcePath.toAbsolutePath(), Paths.get(pathNameInZip));
               }
            }
         }
         zipOutputStream.putArchiveEntry(zipEntry);
         try (final BufferedInputStream fileIS = new BufferedInputStream(new FileInputStream(file))) {
            IOUtils.copy(fileIS, zipOutputStream);
         } catch (FileNotFoundException ex) {
            throw new MojoExecutionException("Error compressing " + file + ".", ex);
         }
      } catch (IOException ex) {
         throw new MojoExecutionException("Error compressing " + file + ".", ex);
      } finally {
         try {
            zipOutputStream.closeArchiveEntry();
         } catch (IOException ex) {
            throw new MojoExecutionException("Error compressing " + file + ".", ex);
         }
      }
   }
}
