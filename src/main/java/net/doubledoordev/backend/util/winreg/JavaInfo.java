/*
 * Unless otherwise specified through the '@author' tag or the javadoc
 * at the top of the file or on a specific portion of the code the following license applies:
 *
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.backend.util.winreg;

import net.doubledoordev.backend.util.OSUtils;

import java.util.regex.Pattern;

/**
 * Java Finder by petrucio@stackoverflow(828681) is licensed under a Creative Commons Attribution 3.0 Unported License.
 * Needs WinRegistry.java. Get it at: http://stackoverflow.com/questions/62289/read-write-to-windows-registry-using-java
 *
 * JavaFinder - Windows-specific classes to search for all installed versions of java on this system
 * Author: petrucio@stackoverflow (828681)
 *****************************************************************************/

/**
 * Helper struct to hold information about one installed java version
 * **************************************************************************
 */
public class JavaInfo implements Comparable<JavaInfo>
{
    private static String regex = "[^\\d_.-]";
    public String path; //! Full path to java.exe executable file
    public String version; //! Version string.
    public String origVersion = "";
    public boolean supportedVersion = false;
    public boolean hasJava8;
    public boolean is64bits; //! true for 64-bit javas, false for 32
    public int major, minor, revision, build;

    /**
     * Calls 'javaPath -version' and parses the results
     *
     * @param javaPath: path to a java.exe executable
     *                  **************************************************************************
     */
    public JavaInfo(String javaPath)
    {
        String versionInfo = RuntimeStreamer.execute(new String[]{javaPath, "-version"});
        String[] tokens = versionInfo.split("\"");
        if (tokens.length < 2) this.version = "0.0.0_00";
        else this.version = tokens[1];
        this.origVersion = version;
        this.version = Pattern.compile(regex).matcher(this.version).replaceAll("0");
        this.is64bits = versionInfo.toUpperCase().contains("64-");
        this.path = javaPath;

        String[] s = this.version.split("[._-]");
        this.major = Integer.parseInt(s[0]);
        this.minor = s.length > 1 ? Integer.parseInt(s[1]) : 0;
        this.revision = s.length > 2 ? Integer.parseInt(s[2]) : 0;
        this.build = s.length > 3 ? Integer.parseInt(s[3]) : 0;

        if (OSUtils.getCurrentOS() == OSUtils.OS.MACOSX)
        {
            if (this.major == 1 && (this.minor == 7 || this.minor == 6)) this.supportedVersion = true;
        }
        else
        {
            this.supportedVersion = true;
        }
    }

    public JavaInfo(int major, int minor)
    {
        this.path = null;
        this.major = major;
        this.minor = minor;
        this.revision = 0;
        this.build = 0;
    }

    public boolean isJava8()
    {
        return this.major == 1 && this.minor == 8;
    }

    /**
     * @return Human-readable contents of this JavaInfo instance
     * **************************************************************************
     */
    public String toString()
    {
        return "Java Version: " + origVersion + " sorted as: " + this.verToString() + " " + (this.is64bits ? "64" : "32") + " Bit Java at : " + this.path + (this.supportedVersion ? "" : " (UNSUPPORTED!)");
    }

    public String verToString()
    {
        return major + "." + minor + "." + revision + "_" + build;
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") JavaInfo o)
    {
        if (o.major > major) return -1;
        if (o.major < major) return 1;
        if (o.minor > minor) return -1;
        if (o.minor < minor) return 1;
        if (o.revision > revision) return -1;
        if (o.revision < revision) return 1;
        if (o.build > build) return -1;
        if (o.build < build) return 1;
        return 0;
    }

}
