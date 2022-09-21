/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.packer;

import org.uggeri.build.tools.ExecutionRequest;
import org.uggeri.build.tools.Version;
import java.io.File;
import java.util.List;

/**
 *
 * @author fabio_uggeri
 */
public interface PackagingRequest extends ExecutionRequest {

   List<File> getSources();

   void setSources(List<File> sources);

   List<File> getLibraries();

   void setLibraries(List<File> libraries);

   String getMainSourceFileName();

   void setMainSourceFileName(String filename);

   String getOutputFileName();

   void setOutputFileName(String filename);

   Version getVersion();

   void setVersion(Version version);
   
   void setPathSeparator(final String pathSeparator);
}
