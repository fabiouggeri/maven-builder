/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.build.tools;

import org.uggeri.build.tools.log.Log;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 *
 * @author ADMIN
 */
public class BuildUtil {

   public static final int DEFAULT_UNIX_MODE = 420; // 0644

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

   public static boolean unzip(File file, String outPath, boolean replaceExistingFiles, Log log) {
      return BuildUtil.unzip(file, outPath, replaceExistingFiles, log, false);
   }

   public static boolean unzip(File file, String outPath, boolean replaceExistingFiles, Log log, boolean flat) {
      try (final ZipFile zipFile = new ZipFile(file)) {
         final Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
         while (entries.hasMoreElements()) {
            final ZipArchiveEntry entry = entries.nextElement();
            final File outfile = new File(outPath, entry.getName());
            if (!outfile.getParentFile().exists()) {
               if (!outfile.getParentFile().mkdirs()) {
                  log.error("Nao foi possivel criar o diretorio " + outfile.getAbsolutePath() + ".");
                  return false;
               }
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
               try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outfile))) {
                  IOUtils.copy(is, os);
               }
            }
            if (isLinux()) {
               Files.setPosixFilePermissions(Paths.get(outfile.getAbsolutePath()), posixPermissions(entry.getUnixMode()));
            }
         }
      } catch (IOException ex) {
         log.error("Erro descompactando o arquivo " + file.getAbsolutePath() + ".", ex);
         return false;
      }
      return true;
   }

   public static String zip(Collection<File> files, File outputFile, Log log, boolean ignoreFirstLevel) {
      return zip(files, outputFile, outputFile.getParentFile(), log, ignoreFirstLevel);
   }

   public static String zip(Collection<File> files, File outputFile, File workDir, Log log, boolean ignoreFirstLevel) {
      String error = null;
      ZipArchiveOutputStream zipOutputStream = null;
      try {
         if (outputFile.exists()) {
            outputFile.delete();
         }
         if (outputFile.createNewFile()) {
            try {
               zipOutputStream = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
               for (File file : files) {
                  error = zipAddFile(zipOutputStream, workDir, file, log, ignoreFirstLevel);
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

   private static String zipAddFile(ZipArchiveOutputStream zipOutputStream, File workDir, File file, Log log,
           boolean ignoreFirstLevel) {
      String error = null;
      ZipArchiveEntry entrada;
      String entryPathName = BuildUtil.getRelativePath(workDir, file);
      if (ignoreFirstLevel) {
         final int sep = entryPathName.indexOf(File.separator);
         if (sep > 0) {
            entryPathName = entryPathName.substring(sep + 1);
         }
      }
      try {
         log.debug("Compactando " + file.getName() + "...");
         entrada = new ZipArchiveEntry(entryPathName);
         if (isLinux()) {
            entrada.setUnixMode(unixMode(Files.getPosixFilePermissions(Paths.get(file.toURI()))));
         }
         zipOutputStream.putArchiveEntry(entrada);
         try (final BufferedInputStream fileIS = new BufferedInputStream(new FileInputStream(file))) {
            IOUtils.copy(fileIS, zipOutputStream);
         }
      } catch (IOException ex) {
         log.debug("Erro compactando " + file.getName() + ".", ex);
         error = ex.getMessage();
      } finally {
         try {
            zipOutputStream.closeArchiveEntry();
         } catch (IOException ex) {
            log.debug("Erro compactando " + file.getName() + ".", ex);
            error = ex.getMessage();
         }
      }
      return error;
   }

   /* Desmembra o path de um arquivo numa lista */
   private static List<String> getPathList(File file) {
      List<String> lista = new ArrayList<>();
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
      List<String> pathNameList = new ArrayList<>(files.size());
      for (File file : files) {
         pathNameList.add(getRelativePath(baseDir, file));
      }
      return pathNameList;
   }

   /**
    * Retorna uma lista com o path e o nome dos arquivos recebidos relativos a um diretorio base.
    *
    * @param baseDir diretorio de base para obtencao do caminho relativo
    * @param files lista de arquivos
    * @param smart verifica qual caminh fica mais curto absoluto ou relativo
    * @return
    */
   public static List<String> fileListToStringList(final File baseDir, final List<File> files, boolean smart) {
      List<String> pathNameList = new ArrayList<>(files.size());
      for (File file : files) {
         final String relativePath = getRelativePath(baseDir, file);
         if (smart && relativePath.length() >= file.getAbsolutePath().length()) {
            pathNameList.add(file.getAbsolutePath());
         } else {
            pathNameList.add(relativePath);
         }
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
      List<String> pathList = new ArrayList<>();
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
      if (isWindows()) {
         if (force || filePathName.indexOf(' ') > 0) {
            return '"' + filePathName + '"';
         }
      } else if (isLinux()) {
         if (force || filePathName.indexOf(' ') > 0) {
            return filePathName.replaceAll(" ", "\\ ");
         }
         
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
      StringBuilder outputPath = new StringBuilder(BuildUtil.removeLastPathSeparator(rootExtractionDir));
      outputPath.append(File.separatorChar);
      outputPath.append(groupId).append(File.separatorChar);
      outputPath.append(artifactId).append(File.separatorChar).append(version);
      if (classifier != null && !classifier.isEmpty()) {
         outputPath.append(File.separatorChar).append(classifier);
      }
      return outputPath.toString();
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
      return BuildUtil.toStringList(files, true);
   }

   public static List<String> toStringList(List<File> files, boolean verifyEnclose) {
      final List<String> list = new ArrayList<>(files.size());
      final boolean winOS = isWindows();
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

   public static boolean isWindows() {
      return System.getProperty("os.name").toLowerCase().contains("win");
   }

   public static boolean isLinux() {
      return System.getProperty("os.name").toLowerCase().contains("linux");
   }

   public static List<File> moveFileToPos(List<File> files, String fileNameToMove, int newPos) {
      final List<File> reorderedList = new ArrayList<>(files.size());
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
         final String path = platformPath(newPath);
         final File f = new File(path);
         if (f.isAbsolute()) {
            return path;
         } else {
            return baseDir.getAbsolutePath() + File.separatorChar + path;
         }
      } else {
         return defaultPath;
      }
   }

   private static int commonFoldersLength(String path1[], String path2[], int maxLen, boolean isCaseSensitive) {
      final int len = Math.min(maxLen, Math.min(path1.length, path2.length));
      int i = 0;
      if (isCaseSensitive) {
         while (i < len) {
            if (!path1[i].equals(path2[i])) {
               break;
            }
            ++i;
         }
      } else {
         while (i < len) {
            if (!path1[i].equalsIgnoreCase(path2[i])) {
               break;
            }
            ++i;
         }
      }
      return i;
   }

   public static File baseDirectory(final List<File> files) {
      final boolean isCaseSensitive = !new File("A").equals(new File("a"));
      String commonFolders[] = null;
      int commonLen = 0;
      for (File f : files) {
         final String filePath = f.isDirectory() ? f.getAbsolutePath() : f.getParentFile().getAbsolutePath();
         final String folders[] = filePath.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
         if (commonFolders == null) {
            commonFolders = folders;
            commonLen = folders.length;
         } else {
            commonLen = commonFoldersLength(commonFolders, folders, commonLen, isCaseSensitive);
         }
      }
      return commonLen > 0 ? new File(arrayToPath(commonFolders, commonLen)) : null;
   }

   private static String arrayToPath(String[] commonPath, int len) {
      final StringBuilder path = new StringBuilder(commonPath.length * 20);
      for (int i = 0; i < len; i++) {
         if (i > 0) {
            path.append(File.separatorChar);
         }
         path.append(commonPath[i]);
      }
      return path.toString();
   }

   public static String platformPath(final String path) {
      if (path == null || path.isBlank()) {
         return "";
      } else if ('\\' == File.separatorChar) {
         return path.replace('/', File.separatorChar);
      } else {
         return path.replace('\\', File.separatorChar);
      }
   }

   public static String platformPathList(String[] paths) {
      final StringBuilder path = new StringBuilder();
      for (String p : paths) {
         if (path.length() > 0) {
            path.append(File.pathSeparatorChar);
         }
         path.append(platformPath(p));
      }
      return path.toString();
   }

   public static int unixMode(Set<PosixFilePermission> s) {
      if (s == null || s.isEmpty()) {
         return DEFAULT_UNIX_MODE;
      }

      int i = 0;
      for (PosixFilePermission p : s) {
         switch (p) {
            case OWNER_EXECUTE:
               i += 64;  // 0100
               break;
            case OWNER_WRITE:
               i += 128; // 0200
               break;
            case OWNER_READ:
               i += 256; // 0400
               break;
            case GROUP_EXECUTE:
               i += 8;   // 0010
               break;
            case GROUP_WRITE:
               i += 16;  // 0020
               break;
            case GROUP_READ:
               i += 32;  // 0040
               break;
            case OTHERS_EXECUTE:
               i += 1;   // 0001
               break;
            case OTHERS_WRITE:
               i += 2;   // 0002
               break;
            case OTHERS_READ:
               i += 4;   // 0004
               break;
         }
      }
      return i;
   }

   public static Set<PosixFilePermission> posixPermissions(int unixMode) {
      if (unixMode <= 0) {
         return Collections.emptySet();
      }

      Set<PosixFilePermission> s = new HashSet<>();

      if ((unixMode & 64) == 64) {   // 0100
         s.add(PosixFilePermission.OWNER_EXECUTE);
      }
      if ((unixMode & 128) == 128) { // 0200
         s.add(PosixFilePermission.OWNER_WRITE);
      }
      if ((unixMode & 256) == 256) { // 0400
         s.add(PosixFilePermission.OWNER_READ);
      }
      if ((unixMode & 8) == 8) {     // 0010
         s.add(PosixFilePermission.GROUP_EXECUTE);
      }
      if ((unixMode & 16) == 16) {   // 0020
         s.add(PosixFilePermission.GROUP_WRITE);
      }
      if ((unixMode & 32) == 32) {   // 0040
         s.add(PosixFilePermission.GROUP_READ);
      }
      if ((unixMode & 1) == 1) {     // 0001
         s.add(PosixFilePermission.OTHERS_EXECUTE);
      }
      if ((unixMode & 2) == 2) {     // 0002
         s.add(PosixFilePermission.OTHERS_WRITE);
      }
      if ((unixMode & 4) == 4) {     // 0004
         s.add(PosixFilePermission.OTHERS_READ);
      }

      return s;
   }
   
   public static String normalizePath(String pathName, String pathSeparator) {
      if ("\\".equals(pathSeparator)) {
         return StringUtils.replace(pathName, "/", pathSeparator);
      } else if ("/".equals(pathSeparator)) {
         return StringUtils.replace(pathName, "\\", pathSeparator);
      } else {
         return StringUtils.replace(StringUtils.replace(pathName, "\\", pathSeparator), "/", pathSeparator);
      }
   }
   
   public static List<String> normalizePath(List<String> pathNames, String pathSeparator) {
      final List<String> paths = new ArrayList<>(pathNames.size());
      for (String p : pathNames) {
         paths.add(normalizePath(p, pathSeparator));
      }
      return paths;
   }
   
   public static String normalizePath(File file, String pathSeparator) {
      return normalizePath(file.getAbsolutePath(), pathSeparator);
   }
}
