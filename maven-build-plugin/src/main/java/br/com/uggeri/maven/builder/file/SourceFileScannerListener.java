/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.uggeri.maven.builder.file;

import java.io.File;

/**
 *
 * @author fabio_uggeri
 */
public interface SourceFileScannerListener {

   public void dirScanStarted(File dir);

   public void fileFound(File file);

   public void fileIncluded(File file);

   public void fileDismissed(File file);

   public void dirScanFinished(File dir);

}
