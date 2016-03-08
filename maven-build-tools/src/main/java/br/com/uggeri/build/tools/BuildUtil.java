/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.build.tools;

import br.com.uggeri.build.tools.log.Log;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public class BuildUtil {

   private static int defaultBufferLength = 32768;

   public static String canonicalPathName(File fileSource) {
      try {
         return fileSource.getCanonicalPath();
      } catch (IOException ex) {
         return "";
      }
   }

   /**
    * Retorna a extensao do nome de um arquivo
    *
    * @param file Arquivo que se deseja obter a extensao do nome
    * @return
    */
   public static String fileExtension(File file) {
      return BuildUtil.fileExtension(file.getName());
   }

   public static String fileExtension(String fileName) {
      int dotIndex = fileName.lastIndexOf(".");
      if (dotIndex == -1) {
         return "";
      } else {
         return fileName.substring(dotIndex + 1, fileName.length());
      }
   }

   /**
    * Retorna somente o nome do arquivo, sem a extensao
    *
    * @param file Arquivo que se deseja obter o nome
    * @return
    */
   public static String nameWithoutExtension(File file) {
      return BuildUtil.removeExtension(file.getName());
   }

   /**
    * Retorna o nome de um arquivo sem sua extensao
    *
    * @param fileName
    * @return
    */
   public static String removeExtension(final String fileName) {
      int dotIndex = fileName.lastIndexOf(".");
      if (dotIndex == -1) {
         return fileName;
      } else {
         return fileName.substring(0, dotIndex);
      }
   }

   public static String removePath(final String fileName) {
      return removePath(fileName, File.separatorChar);
   }

   /**
    * Retorna o nome de um arquivo sem sua extensao
    *
    * @param fileName
    * @param pathSep
    * @return
    */
   public static String removePath(final String fileName, final char pathSep) {
      int dotIndex = fileName.lastIndexOf(pathSep);
      if (dotIndex == -1) {
         return fileName;
      } else {
         return fileName.substring(dotIndex + 1);
      }
   }

   /**
    * Retorna um path sem o caracter seperador de path no final
    *
    * @param path Path a se remover o separador
    * @return
    */
   public static String removeLastPathSeparator(String path) {
      String result = path.trim();
      if (result.charAt(result.length() - 1) == File.separatorChar) {
         result = result.substring(0, result.length() - 1);
      }
      return result;
   }

   /**
    * Cria os diretorios que nao existem e que compoem o path informado
    *
    * @param path Path que tera seus diretorios criados
    * @return
    */
   public static boolean makeDirsPath(String path) {
      File directory = new File(path);
      if (!directory.exists()) {
         return directory.mkdirs();
      }
      return true;
   }

   public static boolean deleteFiles(File dir) {
      return deleteFiles(dir, true);
   }

   public static boolean deleteFiles(File dir, boolean recursive) {
      boolean success = true;
      if (dir != null && dir.isDirectory()) {
         File[] files = dir.listFiles();
         for (File file : files) {
            if (file.isDirectory()) {
               if (recursive) {
                  success = success && deleteFiles(file, true);
               }
            } else {
               success = success && file.delete();
            }
         }
         return success;
      }
      return success;
   }

   public static boolean deleteDirectory(File dir) {
      if (dir != null && dir.isDirectory()) {
         File[] files = dir.listFiles();
         for (File file : files) {
            if (file.isDirectory()) {
               deleteDirectory(file);
            } else {
               file.delete();
            }
         }
         return dir.delete();
      }
      return false;
   }

   private static void copyData(final InputStream in, final OutputStream out, final int bufferLength) throws IOException {
      byte[] buffer = new byte[bufferLength];
      int len;

      while ((len = in.read(buffer)) >= 0) {
         out.write(buffer, 0, len);
      }
      in.close();
      out.close();
   }

   public static boolean extractZip(File file, String outPath, boolean replaceExistingFiles, Log log) {
      return extractZip(file, outPath, replaceExistingFiles, log, false);
   }

   public static boolean extractZip(File file, String outPath, boolean replaceExistingFiles, Log log, boolean flat) {
      try {
         ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);
         Enumeration<? extends ZipEntry> entries = zipFile.entries();

         while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            File unzipedFile = new File(outPath, zipEntry.getName());

            if (zipEntry.isDirectory() && !flat) {
               log.debug("Extraindo diretorio: " + zipEntry.getName());
               if (!unzipedFile.exists()) {
                  if (!unzipedFile.mkdirs()) {
                     log.debug("Nao foi possivel criar o diretorio " + unzipedFile.getAbsolutePath() + ".");
                     return false;
                  }
               }
            } else {
               File dirFile;
               if (flat) {
                  unzipedFile = new File(outPath, unzipedFile.getName());
               } else {
                  dirFile = new File(unzipedFile.getParent());
                  if (!dirFile.exists()) {
                     if (!dirFile.mkdirs()) {
                        log.debug("Nao foi possivel criar o diretorio " + dirFile.getAbsolutePath() + ".");
                        return false;
                     }
                  }
               }
               if (unzipedFile.exists()) {
                  if (replaceExistingFiles) {
                     unzipedFile.delete();
                  } else {
                     continue;
                  }
               }
               log.debug("Extraindo o arquivo: " + zipEntry.getName());
               copyData(zipFile.getInputStream(zipEntry), new BufferedOutputStream(new FileOutputStream(unzipedFile)), getDefaultBufferLength());
            }
         }
         zipFile.close();
      } catch (IOException ex) {
         log.debug("Erro descompactando o arquivo " + file.getAbsolutePath() + ".", ex);
         return false;
      }
      return true;
   }

   public static String createZip(Collection<File> files, File outputFile, Log log) {
      return createZip(files, outputFile, outputFile.getParentFile(), log);
   }

   public static String createZip(Collection<File> files, File outputFile, File workDir, Log log) {
      String error = null;
      ZipOutputStream zipOutputStream = null;
      try {
         if (outputFile.exists()) {
            outputFile.delete();
         }
         if (outputFile.createNewFile()) {
            try {
               zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
               for (File file : files) {
                  error = compactFile(zipOutputStream, workDir, file, log);
                  if (error != null) {
                     break;
                  }
               }
            } catch (FileNotFoundException ex) {
               error = ex.getMessage();
               log.debug("Erro ao criar " + outputFile.getName() + ".", ex);
            } finally {
               if (zipOutputStream != null) {
                  try {
                     zipOutputStream.close();
                  } catch (IOException ex) {
                     error = ex.getMessage();
                     log.debug("Erro ao criar " + outputFile.getName() + ".", ex);
                  }
               }
            }
         }
      } catch (IOException ex) {
         return ex.getMessage();
      }
      return error;
   }

   private static String compactFile(ZipOutputStream zipOutputStream, File workDir, File file, Log log) {
      int bytesLidos;
      String error = null;
      byte[] buffer = new byte[8192];
      BufferedInputStream fileIS;
      try {
         fileIS = new BufferedInputStream(new FileInputStream(file));
         ZipEntry entrada = new ZipEntry(BuildUtil.getRelativePath(workDir, file));
         try {
            log.debug("Compactando " + file.getName() + "...");
            zipOutputStream.putNextEntry(entrada);
            while ((bytesLidos = fileIS.read(buffer)) != -1) {
               zipOutputStream.write(buffer, 0, bytesLidos);
            }
         } catch (IOException ex) {
            log.debug("Erro compactando " + file.getName() + ".", ex);
            error = ex.getMessage();
         } finally {
            try {
               fileIS.close();
               zipOutputStream.closeEntry();
            } catch (IOException ex) {
               log.debug("Erro compactando " + file.getName() + ".", ex);
               error = ex.getMessage();
            }
         }
      } catch (FileNotFoundException ex) {
         log.debug("Erro compactando " + file.getName() + ".", ex);
         error = ex.getMessage();
      }
      return error;
   }

   /* Desmembra o path de um arquivo numa lista */
   private static List<String> getPathList(File file) {
      List<String> lista = new ArrayList<String>();
      File parent;
      try {
         parent = file.getCanonicalFile();
         while (parent != null) {
            lista.add(parent.getName());
            parent = parent.getParentFile();
         }
      } catch (IOException e) {
         lista = null;
      }
      return lista;
   }

   /**
    * @param base Diretorio base para construcao do caminho relativo
    * @param target Diretorio ou arquivo a ter seu path transformado em relativo
    */
   private static String relativePath(List<String> base, List<String> target) {
      int baseIndex;
      int targetIndex;
      StringBuilder relativePathName = new StringBuilder();

      // Inicia no fim das listas, que onde esta o parente mais proximo
      baseIndex = base.size() - 1;
      targetIndex = target.size() - 1;

      // Primeiro elimina a parte comum...
      while (baseIndex >= 0 && targetIndex >= 0 && base.get(baseIndex).equals(target.get(targetIndex))) {
         --baseIndex;
         --targetIndex;
      }

      // Para cada nivel restante adiciona ..
      for (; baseIndex >= 0; baseIndex--) {
         relativePathName.append("..").append(File.separator);
      }

      // Adiciona os niveis restantes do arquivo/diretorio ao path
      for (; targetIndex >= 1; targetIndex--) {
         relativePathName.append(target.get(targetIndex)).append(File.separator);
      }

      // Nome do arquivo
      if (targetIndex >= 0) {
         relativePathName.append(target.get(targetIndex));
      }
      return relativePathName.toString();
   }

   /**
    * Retorna o path relativo do arquivo 'target' em relacao ao diretorio 'base' Exemplo : base = /a/b/c target = /a/d/e/x.txt
    * getRelativePath(base,target) = ../../d/e/x.txt
    *
    * @param base diretorio base para montagem do caminho relativo
    * @param target diretorio/arquivo para o qual se deseja montar o caminho relativo
    * @return caminho relativo de base para target como uma string
    */
   public static String getRelativePath(File base, File target) {
      return relativePath(getPathList(base), getPathList(target));
   }

   /**
    * Retorna o path relativo do arquivo 'target' em relacao ao diretorio 'base' Exemplo : base = /a/b/c target = /a/d/e/x.txt
    * getRelativePath(base,target) = ../../d/e/x.txt
    *
    * @param base diretorio base para montagem do caminho relativo
    * @param target diretorio/arquivo para o qual se deseja montar o caminho relativo
    * @param enclose indica se o path deve ser retornado entre aspas caso contenha espacos
    * @return caminho relativo de base para target como uma string
    */
   public static String getRelativePath(File base, File target, boolean enclose) {
      if (enclose) {
         return encloseFilePathName(relativePath(getPathList(base), getPathList(target)), false);
      } else {
         return relativePath(getPathList(base), getPathList(target));
      }
   }

   /**
    * Retorna se os arquivos informados apontam para o mesmo objeto no sistema de arquivos.
    *
    * @param file1 primeiro arquivo a ser comparado
    * @param file2 segundo arquivo a ser comparado
    * @return
    */
   public static boolean isSameFile(final File file1, final File file2) {
      return canonicalPathName(file1).equals(canonicalPathName(file2));
   }

   /**
    * Retorna uma lista com o path e o nome dos arquivos recebidos relativos a um diretorio base.
    *
    * @param baseDir diretorio de base para obtencao do caminho relativo
    * @param files lista de arquivos
    * @return
    */
   public static List<String> fileListToStringList(final File baseDir, final List<File> files) {
      List<String> pathNameList = new ArrayList<String>(files.size());
      for (File file : files) {
         pathNameList.add(getRelativePath(baseDir, file));
      }
      return pathNameList;
   }

   private static void traverseTree(final List<String> pathList, final String curDir, final FileFilter filter) {
      File curDirFile = new File(curDir);
      File[] files = curDirFile.listFiles(filter);
      pathList.add(curDir);
      for (File file : files) {
         traverseTree(pathList, file.getAbsolutePath(), filter);
      }
   }

   public static List<String> directoriesPathList(final File directory) {
      List<String> pathList = new ArrayList<String>();
      if (directory.isDirectory()) {
         FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
               return pathname.isDirectory();
            }
         };
         traverseTree(pathList, canonicalPathName(directory), filter);
      }
      return pathList;
   }

   public static String filesPath(Collection<File> files) {
      final StringBuilder sb = new StringBuilder();
      for (File file : files) {
         if (sb.length() > 0) {
            sb.append(File.pathSeparatorChar);
         }
         sb.append(canonicalPathName(file.getParentFile()));
      }
      return sb.toString();
   }

   public static String encloseFilePathName(String filePathName) {
      return encloseFilePathName(filePathName, true);
   }

   public static String encloseFilePathName(String filePathName, boolean force) {
      if (force || filePathName.indexOf(' ') > 0) {
         return '"' + filePathName + '"';
      }
      return filePathName;
   }

   public static Commandline createCommandLine(String commandLine) {
      return new Commandline(commandLine.replace("\"", "\'\"\'"));
   }

   public static boolean isChildFile(File directory, File file) {
      return directory.isDirectory() && canonicalPathName(file).startsWith(canonicalPathName(directory));
   }

   public static boolean isChildFile(String directory, File file) {
      return isChildFile(new File(directory), file);
   }

   public static boolean moveFile(File fromFile, File toFile, boolean overwrite) {
      boolean move = false;
      if (fromFile.exists() && !toFile.isDirectory()) {
         move = true;
         if (toFile.exists()) {
            if (overwrite) {
               move = toFile.delete();
            } else {
               move = false;
            }
         }
         if (move) {
            BufferedInputStream fileIS = null;
            BufferedOutputStream fileOS = null;
            try {
               fileIS = new BufferedInputStream(new FileInputStream(fromFile));
               fileOS = new BufferedOutputStream(new FileOutputStream(toFile));
               copyData(fileIS, fileOS, getDefaultBufferLength());
            } catch (FileNotFoundException ex) {
               move = false;
            } catch (IOException ex) {
               move = false;
            } finally {
               if (fileIS != null) {
                  try {
                     fileIS.close();
                  } catch (IOException ex) {
                  }
               }
               if (fileOS != null) {
                  try {
                     fileOS.close();
                  } catch (IOException ex) {
                  }
               }
               if (move) {
                  move = fromFile.delete();
               }
            }
         }
      }
      return move;
   }

   /**
    * @return the defaultBufferLength
    */
   public static int getDefaultBufferLength() {
      return defaultBufferLength;
   }

   /**
    * @param aDefaultBufferLength the defaultBufferLength to set
    */
   public static void setDefaultBufferLength(int aDefaultBufferLength) {
      defaultBufferLength = aDefaultBufferLength;
   }

   public static String pathToExtractArtifact(final String rootExtractionDir, final String groupId, final String artifactId, final String version, final String classifier) {
      StringBuilder pathIncludes = new StringBuilder(BuildUtil.removeLastPathSeparator(rootExtractionDir));
      pathIncludes.append(File.separatorChar);
      pathIncludes.append(groupId.replace('.', File.separatorChar)).append(File.separatorChar);
      pathIncludes.append(artifactId).append(File.separatorChar).append(version);
      if (classifier != null && !classifier.isEmpty()) {
         pathIncludes.append(File.separatorChar).append(classifier);
      }
      return pathIncludes.toString();
   }

   public static boolean createArtifactExtractionDirectory(final Log log, final File artifactFile, final File dirExtract) {
      /* Se diretorio nao existe, tenta criar... */
      if (!dirExtract.exists()) {
         if (!dirExtract.mkdirs()) {
            log.debug("Nao foi possivel criar o diretorio " + dirExtract.getAbsolutePath() + ".");
            return false;
         }
      } else if (artifactFile.lastModified() > dirExtract.lastModified()) {
         if (BuildUtil.deleteDirectory(dirExtract)) {
            if (!dirExtract.mkdirs()) {
               log.debug("Nao foi possivel criar o diretorio " + dirExtract.getAbsolutePath() + ".");
               return false;
            }
         } else {
            log.debug("Nao foi possivel remover o diretorio " + dirExtract.getAbsolutePath() + ".");
            return false;
         }
      }
      return true;
   }

   public static List<String> toStringList(List<File> files) {
      final List<String> list = new ArrayList<String>(files.size());
      final boolean winOS = System.getProperty("os.name").toLowerCase().contains("win");
      for (File file : files) {
         final String absolutePathName = file.getAbsolutePath();
         if (absolutePathName.indexOf(' ') >= 0) {
            if (winOS) {
               list.add(BuildUtil.encloseFilePathName(absolutePathName));
            } else {
               list.add(absolutePathName.replace(" ", "\\ "));
            }
         } else {
            list.add(absolutePathName);
         }
      }
      return list;
   }

   public static List<File> moveFileToPos(List<File> files, String fileNameToMove, int newPos) {
      final List<File> reorderedList = new ArrayList<File>(files.size());
      File fileToMove = null;
      boolean hasExtension = !fileExtension(fileNameToMove).isEmpty();
      for (File file : files) {
         final String fileName = hasExtension ? file.getName() : nameWithoutExtension(file);
         if (fileNameToMove.equalsIgnoreCase(fileName)) {
            fileToMove = file;
         } else {
            reorderedList.add(file);
         }
      }
      if (fileToMove != null) {
         reorderedList.add(newPos, fileToMove);
      }
      return reorderedList;
   }

   public static String configDirectory(final File baseDir, final String newPath, final String defaultPath) {
      if (newPath != null && !newPath.isEmpty()) {
         final File f = new File(newPath);
         if (f.isAbsolute()) {
            return newPath;
         } else {
            return baseDir.getAbsolutePath() + File.separatorChar + newPath;
         }
      } else {
         return defaultPath;
      }
   }
}
