/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uggeri.build.tools;

import org.uggeri.build.tools.packer.Packer;
import org.uggeri.build.tools.packer.zip.ZipPacker;

/**
 *
 * @author ADMIN
 */
public enum PackagingType {

   // TODO: DLL

   LIB {
      @Override
      public String toString() {
         return "lib";
      }

      @Override
      public boolean isNeedPackage() {
         return true;
      }

      @Override
      public boolean isNeedCompile() {
         return true;
      }

      @Override
      public Packer getDefaultPacker() {
         return null;
      }
   },

   EXE {
      @Override
      public String toString() {
         return "exe";
      }

      @Override
      public boolean isNeedPackage() {
         return true;
      }

      @Override
      public boolean isNeedCompile() {
         return true;
      }

      @Override
      public Packer getDefaultPacker() {
         return null;
      }
   },

   PLSQL {
      private final Packer defaultPacker = new ZipPacker();

      @Override
      public String toString() {
         return "plsql";
      }

      @Override
      public boolean isNeedPackage() {
         return true;
      }

      @Override
      public boolean isNeedCompile() {
         return false;
      }

      @Override
      public Packer getDefaultPacker() {
         return defaultPacker;
      }
   },

   INCLUDE {
      private final Packer defaultPacker = new ZipPacker();

      @Override
      public String toString() {
         return "include";
      }

      @Override
      public boolean isNeedPackage() {
         return true;
      }

      @Override
      public boolean isNeedCompile() {
         return false;
      }

      @Override
      public Packer getDefaultPacker() {
         return defaultPacker;
      }
   },

   FILE {
      @Override
      public String toString() {
         return "file";
      }

      @Override
      public boolean isNeedPackage() {
         return false;
      }

      @Override
      public boolean isNeedCompile() {
         return false;
      }

      @Override
      public Packer getDefaultPacker() {
         return null;
      }
   };

   public static PackagingType getByName(String typeName) {
      for(PackagingType value : PackagingType.values()) {
         if (value.name().equalsIgnoreCase(typeName)) {
            return value;
         }
      }
      return null;
   }

   public abstract boolean isNeedPackage();

   public abstract boolean isNeedCompile();

   public abstract Packer getDefaultPacker();

}
