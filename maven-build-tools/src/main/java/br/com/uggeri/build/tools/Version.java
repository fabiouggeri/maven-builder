/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools;

import java.util.Arrays;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author fabio_uggeri
 */
public class Version implements Comparable<Version> {

   public enum Status {
      SNAPSHOT {
         @Override
         public String suffix() {
            return "-SNAPSHOT";
         }
      },

      ALPHA {
         @Override
         public String suffix() {
            return "-ALPHA";
         }
      },

      BETA {
         @Override
         public String suffix() {
            return "-BETA";
         }
      },

      RELEASE_CANDIDATE {
         @Override
         public String suffix() {
            return "-RC";
         }
      },

      RELEASE {
         @Override
         public String suffix() {
            return "";
         }
      };

      public abstract String suffix();
   }

   private int major;
   private int minor;
   private int fix;
   private int build;
   private Status status;

   public Version(final int major, final int minor, final int fix, final int build, Status status) {
      setMajor(major);
      setMinor(minor);
      setFix(fix);
      setBuild(build);
      setStatus(status);
   }

   public Version(final int major, final int minor, final int fix, final int build) {
      this(major, minor, fix, build, Status.SNAPSHOT);
   }

   public Version(final int major, final int minor, final int fix) {
      this(major, minor, fix, 0);
   }

   public Version(final int major, final int minor) {
      this(major, minor, 0);
   }

   public Version(final Version other) {
      this(other.major, other.minor, other.fix, other.build);
   }

   /**
    * @return the major
    */
   public int getMajor() {
      return major;
   }

   /**
    * @param major the major to set
    */
   public final void setMajor(int major) {
      if (major >= 0) {
         this.major = major;
      } else {
         this.major = 0;
      }
   }

   /**
    * @return the minor
    */
   public int getMinor() {
      return minor;
   }

   /**
    * @param minor the minor to set
    */
   public final void setMinor(int minor) {
      if (minor >= 0) {
         this.minor = minor;
      } else {
         this.minor = 0;
      }
   }

   /**
    * @return the fix
    */
   public int getFix() {
      return fix;
   }

   /**
    * @param fix the fix to set
    */
   public final void setFix(int fix) {
      if (fix >= 0) {
         this.fix = fix;
      } else {
         this.fix = 0;
      }
   }

   public boolean isMajor() {
      return ! isMinor() && ! isFix();
   }

   public boolean isMinor() {
      return ! isFix() && getMinor() > 0;
   }

   public boolean isFix() {
      return getFix() > 0;
   }

   public Version nextMajor() {
      return new Version(getMajor() + 1, 0);
   }

   public Version nextMinor() {
      return new Version(getMajor(), getMinor() + 1);
   }

   public Version nextFix() {
      return new Version(getMajor(), getMinor(), getFix() + 1);
   }

   public Version nextBuild() {
      return new Version(getMajor(), getMinor(), getFix(), getBuild() + 1);
   }

   public Version getMajorVersion() {
      return new Version(getMajor(), 0);
   }

   public Version getMinorVersion() {
      return new Version(getMajor(), getMinor());
   }

   public boolean isFixOf(Version otherVersion) {
      if (otherVersion != null) {
         return ! otherVersion.isFix() &&
                 otherVersion.getMajor() == getMajor() &&
                 otherVersion.getMinor() == getMinor() &&
                 getFix() > 0;
      }
      return false;
   }

   public boolean isLater(Version otherVersion) {
      if (otherVersion != null) {
         return compareTo(otherVersion) > 0;
      }
      return false;
   }

   public boolean isEarlier(Version otherVersion) {
      if (otherVersion != null) {
         return compareTo(otherVersion) < 0;
      }
      return false;
   }

   public static Version fromArray(final int... parts) {
      switch(parts.length) {
         case 1:
            return new Version(parts[0], 0);
         case 2:
            return new Version(parts[0], parts[1]);
         case 3:
            return new Version(parts[0], parts[1], parts[2]);
         case 4:
            return new Version(parts[0], parts[1], parts[2], parts[3]);
      }
      return null;
   }

   public static Version fromInvertedArray(final int... parts) {
      switch(parts.length) {
         case 1:
            return new Version(parts[0], 0);
         case 2:
            return new Version(parts[1], parts[0]);
         case 3:
            return new Version(parts[2], parts[1], parts[0]);
         case 4:
            return new Version(parts[3], parts[2], parts[1], parts[0]);
      }
      return null;
   }

   /** Monta o numero da versao de tras para frente com pedacos de tamanhos fixos.
    * Ex.: 100   == 1.0
    *      10103 == 1.1.3
    *      10001 == 1.0.
    * @param formattedVersion
    * @param size
    * @return */
   public static Version parseFixed(final String formattedVersion, final int size) {
      Version version = null;
      if (formattedVersion != null) {
         final String versionNumber;
         final int statusSep = formattedVersion.lastIndexOf('-');
         final Status status;
         final int[] numPieces = new int[]{0, 0, 0, 0};
         int index = 0;
         int value = 0;
         int len;
         if (statusSep > 0) {
            versionNumber = formattedVersion.substring(0, statusSep);
            status = statusFromSufix(formattedVersion.substring(statusSep));
         } else {
            versionNumber = formattedVersion;
            status = Status.RELEASE;
         }
         len = size - (versionNumber.length() % size);
         for (int i = 0; i < versionNumber.length(); i++) {
            value = (value * 10) + (formattedVersion.charAt(i) - '0');
            ++len;
            if (len >= size) {
               numPieces[index++] = value;
               len = 0;
               value = 0;
            }
         }
         version = Version.fromArray(numPieces);
         version.setStatus(status);
      }
      return version;
   }

   private static Status statusFromSufix(final String suffix) {
      for(Status s : Status.values()) {
         if (s.suffix().equals(suffix)) {
            return s;
         }
      }
      return Status.RELEASE;
   }

   public static Version parse(final String formattedVersion) {
      if (formattedVersion != null) {
         try {
            final String versionNumber;
            final int index = formattedVersion.lastIndexOf('-');
            final Status status;
            final Version version;
            if (index > 0) {
               versionNumber = formattedVersion.substring(0, index);
               status = statusFromSufix(formattedVersion.substring(index));
            } else {
               versionNumber = formattedVersion;
               status = Status.RELEASE;
            }

            String[] strPieces = versionNumber.split("\\.");
            if (strPieces.length > 0 && strPieces.length < 5) {
               int[] numPieces = new int[4];
               Arrays.fill(numPieces, 0);
               for(int i = 0; i < strPieces.length; i++) {
                  numPieces[i] = Integer.parseInt(strPieces[i]);
               }
               version = Version.fromArray(numPieces);
               version.setStatus(status);
               return version;
            }
         } catch (NumberFormatException ex) {
            ex.printStackTrace(System.out);
         }
      }
      return null;
   }

   public boolean isEmpty() {
      return major == 0 && minor == 0 && fix == 0 && build == 0;
   }

   @Override
   public String toString() {
      return getNumber() + getStatus().suffix();
   }

   public String getNumber() {
      String version = major + "." + minor;
      if (fix > 0) {
         version += "." + fix;
         if (build > 0) {
            version += "." + build;
         }
      }
      return version;
   }

   public String toStringFixed(final int size) {
      StringBuilder sb = new StringBuilder();
      sb.append(major);
      sb.append('.').append(StringUtils.leftPad(Integer.toString(minor), size, "0"));
      if (fix > 0) {
         sb.append('.').append(StringUtils.leftPad(Integer.toString(fix), size, "0"));
         if (build > 0) {
            sb.append('.').append(StringUtils.leftPad(Integer.toString(build), size, "0"));
         }
      }
      return sb.toString();
   }

   public boolean isStatus(Status status) {
      return getStatus().equals(status);
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof Version) {
         return compareTo((Version)o) == 0;
      }
      return false;
   }

   @Override
   public int hashCode() {
      int hash = 7;
      hash = 53 * hash + this.major;
      hash = 53 * hash + this.minor;
      hash = 53 * hash + this.fix;
      hash = 53 * hash + this.build;
      hash = 53 * hash + this.status.ordinal();
      return hash;
   }

   @Override
   public int compareTo(Version o) {
      int result = getMajor() - o.getMajor();
      if (result == 0) {
         result = getMinor() - o.getMinor();
         if (result == 0) {
            result = getFix() - o.getFix();
            if (result == 0) {
               result = getBuild() - o.getBuild();
            }
         }
      }
      return Integer.signum(result);
   }

   /**
    * @return the build
    */
   public int getBuild() {
      return build;
   }

   /**
    * @param build the build to set
    */
   public final void setBuild(int build) {
      this.build = build;
   }

   /**
    * @return the status
    */
   public Status getStatus() {
      return status;
   }

   /**
    * @param status the status to set
    */
   public final void setStatus(Status status) {
      this.status = status;
   }
}
