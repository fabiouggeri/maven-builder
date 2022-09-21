/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools.packer;

import org.uggeri.build.tools.ExecutionResult;
import java.io.File;

/**
 *
 * @author fabio_uggeri
 */
public interface PackagingResult extends ExecutionResult {
   
   public File getOutputFile();
}
