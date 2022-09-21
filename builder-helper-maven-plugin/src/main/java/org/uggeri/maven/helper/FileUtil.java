/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uggeri.maven.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public class FileUtil {

   /**
    * Metodo que retorna o nome de um arquivo sem seu path e opcionalmente sem sua extensao.
    *
    * @param filePathName Nome do arquivo com seu path
    * @param withExtension Indica se deve retornar o nome do arquivo com ou sem extensao
    * @param o nome do arquivo com ou sem extensao
    */
   public static String extractFileName(String filePathName, boolean withExtension) {
      int start = filePathName.lastIndexOf(File.separatorChar);
      int end;

      if (withExtension) {
         end = filePathName.length();
      } else {
         end = filePathName.indexOf('.');
         if (end < 0) {
            end = filePathName.length();
         }
      }
      return filePathName.substring(start + 1, end);
   }

   public static String getExtension(String filePathName) {
      int index = filePathName.lastIndexOf('.');
      if (index >= 0) {
         return filePathName.substring(index + 1);
      }
      return "";
   }

   public static String normalize(String filePathName) {
      return normalize(filePathName, File.separator);
   }

   public static String normalize(final String filePathName, final String separator) {
      if (filePathName != null) {
         if (filePathName.startsWith(separator)) {
            if (filePathName.endsWith(separator)) {
               return filePathName.substring(0, filePathName.length() - separator.length());
            }
         } else {
            if (filePathName.endsWith(separator)) {
               return separator + filePathName.substring(0, filePathName.length() - separator.length());
            } else {
               return separator + filePathName;
            }
         }
      }
      return filePathName;
   }

   public static void copyFile(File fromFile, File toFile) throws IOException {
      copyFile(fromFile, toFile, 4096);
   }

   public static void copyFile(File fromFile, File toFile, int bufferSize) throws IOException {
      if (!fromFile.exists()) {
         throw new IOException("FileUtil.copyFile: arquivo de origem nao encontrado: " + fromFile.getAbsolutePath());
      }
      if (!fromFile.isFile()) {
         throw new IOException("FileUtil.copyFile: não é possível copiar diretórios: " + fromFile.getAbsolutePath());
      }
      if (!fromFile.canRead()) {
         throw new IOException("FileUtil.copyFile: arquivo de origem não pode ser lido: " + fromFile.getAbsolutePath());
      }

      if (toFile.isDirectory()) {
         toFile = new File(toFile, fromFile.getName());
      }

      if (toFile.exists()) {
         throw new IOException("FileUtil.copyFile: arquivo de destino já existe: " + toFile.getAbsolutePath());
      } else {
         String parent = toFile.getParent();
         if (parent == null) {
            parent = System.getProperty("user.dir");
         }
         File dir = new File(parent);
         if (!dir.exists()) {
            throw new IOException("FileUtil.copyFile: diretório de destino não existe: " + parent);
         }
         if (dir.isFile()) {
            throw new IOException("FileUtil.copyFile: destino não é um diretório: " + parent);
         }
         if (!dir.canWrite()) {
            throw new IOException("FileUtil.copyFile: diretório de destino não pode ser escrito: " + parent);
         }
      }

      BufferedInputStream fromStream = null;
      BufferedOutputStream toStream = null;
      try {
         fromStream = new BufferedInputStream(new FileInputStream(fromFile));
         toStream = new BufferedOutputStream(new FileOutputStream(toFile));
         byte[] buffer = new byte[bufferSize];
         int bytesRead;

         while ((bytesRead = fromStream.read(buffer)) != -1) {
            toStream.write(buffer, 0, bytesRead);
         }
      } finally {
         if (fromStream != null) {
            fromStream.close();
         }
         if (toStream != null) {
            toStream.close();
         }
      }
   }

   /**
    * Funcao para remocao de um arquivo ou diretorio com ou sem conteudo.
    *
    * @param file arquivo ou diretorio a ser removido
    * @return true em caso de sucesso ou false caso contrario
    */
   public static boolean deleteFile(final File file) {
      if (file.isDirectory()) {
         for (File child : file.listFiles()) {
            deleteFile(child);
         }
         return file.delete();
      } else {
         return file.delete();
      }
   }

   /* Desmembra o path de um arquivo numa lista */
   private static List<String> getPathList(File file) {
      List<String> lista = new ArrayList<String>();
      File parent;
      parent = file.getAbsoluteFile();
      while (parent != null) {
         lista.add(parent.getName());
         parent = parent.getParentFile();
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
    * Retorna se os arquivos informados apontam para o mesmo objeto no sistema de arquivos.
    *
    * @param file1 primeiro arquivo a ser comparado
    * @param file2 segundo arquivo a ser comparado
    */
   public static boolean isSameFile(final File file1, final File file2) {
      return canonicalPathName(file1).equals(canonicalPathName(file2));
   }

   /**
    * Retorna uma lista com o path e o nome dos arquivos recebidos com base num diretorio base.
    *
    * @param baseDir diretorio de base para obtencao do caminho relativo
    * @param files lista de arquivos
    */
   public static List<String> fileListToStringList(final File baseDir, final List<File> files) {
      List<String> pathNameList = new ArrayList<String>(files.size());
      for (File file : files) {
         pathNameList.add(getRelativePath(baseDir, file));
      }
      return pathNameList;
   }

   public static String canonicalPathName(File fileSource) {
      try {
         return fileSource.getCanonicalPath();
      } catch (IOException ex) {
         return "";
      }
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
}
