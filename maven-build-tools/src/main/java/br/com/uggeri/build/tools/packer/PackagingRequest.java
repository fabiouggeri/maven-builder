/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer;

import br.com.uggeri.build.tools.ExecutionRequest;
import br.com.uggeri.build.tools.Version;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fabio_uggeri
 */
public interface PackagingRequest extends ExecutionRequest {

   public List<File> getSources();

   public void setSources(List<File> sources);

   public List<File> getLibraries();

   public void setLibraries(List<File> libraries);

   public String getMainSourceFileName();

   public void setMainSourceFileName(String filename);

   public String getOutputFileName();

   public void setOutputFileName(String filename);

   public Version getVersion();

   public void setVersion(Version version);

   public Map<String, String> getEnvironmentVariables();

   public void setEnvironmentVariables(Map<String, String> environmentVariables);
}
