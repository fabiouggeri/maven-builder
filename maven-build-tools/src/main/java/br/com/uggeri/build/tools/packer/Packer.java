/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.packer;

import br.com.uggeri.build.tools.Tool;
import java.util.Collection;

/**
 *
 * @author ADMIN
 */
public interface Packer extends Tool<PackagingResult, PackagingRequest> {

   String getDefaultOutputExtension();

   Collection<String> getSupportedPackagings();
}
