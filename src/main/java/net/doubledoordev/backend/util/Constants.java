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

package net.doubledoordev.backend.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.winreg.JavaFinder;
import net.doubledoordev.backend.util.winreg.JavaInfo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Dries007
 */
public class Constants
{
    public static final String NAME = "D3 Backend";
    public static final String LOCALHOST = "localhost";

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Server.class, new Server.Deserializer())
            .registerTypeAdapter(Server.class, new Server.Serializer())
            .setPrettyPrinting()
            .create();
    public static final Random RANDOM = new Random();
    public static final String JAVAPATH = getJavaPath();

    public static final File ROOT = getRootFile();
    public static final File CONFIG_FILE = new File(ROOT, "config.json");
    public static final File SERVERS = new File(ROOT, "servers");

    private static final char[] symbols = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static File getRootFile()
    {
        try
        {
            return new File(".").getCanonicalFile();
        }
        catch (IOException e)
        {
            return new File(".");
        }
    }

    public static String randomString(int length)
    {
        return new String(randomCharArray(length));
    }

    public static char[] randomCharArray(int length)
    {
        if (length < 1) throw new IllegalArgumentException("length < 1: " + length);

        final char[] buf = new char[length];

        for (int idx = 0; idx < buf.length; ++idx) buf[idx] = symbols[RANDOM.nextInt(symbols.length)];
        return buf;
    }


    private static String getJavaPath()
    {
        JavaInfo javaVersion;
        if (OSUtils.getCurrentOS() == OSUtils.OS.MACOSX)
        {
            javaVersion = JavaFinder.parseJavaVersion();

            if (javaVersion != null && javaVersion.path != null) return javaVersion.path;
        }
        else if (OSUtils.getCurrentOS() == OSUtils.OS.WINDOWS)
        {
            javaVersion = JavaFinder.parseJavaVersion();

            if (javaVersion != null && javaVersion.path != null) return javaVersion.path.replace(".exe", "w.exe");
        }

        // Windows specific code adds <java.home>/bin/java no need mangle javaw.exe here.
        return System.getProperty("java.home") + "/bin/java";
    }
}
