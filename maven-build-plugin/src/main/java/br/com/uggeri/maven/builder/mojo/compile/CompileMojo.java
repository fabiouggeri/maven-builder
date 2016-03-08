package br.com.uggeri.maven.builder.mojo.compile;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.File;
import java.util.*;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Compile source files into native object files
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CompileMojo extends AbstractCompileMojo {

   @Override
   protected List<File> getSourceFiles() {
      return getBuildContext().getSourceFiles();
   }

   @Override
   protected String getCompilationOutputDir() {
      return getBuildContext().getOutputDirectory();
   }

   @Override
   protected String getSourcesDir() {
      return getBuildContext().getSourceDirectory();
   }

   @Override
   protected void setOutputFiles(List<File> files) {
      getBuildContext().setSourceFiles(files);
   }

   @Override
   protected boolean isSkipCompilation() {
      return false;
   }
}
