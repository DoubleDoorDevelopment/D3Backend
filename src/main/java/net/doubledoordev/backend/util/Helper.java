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
 *
 */

package net.doubledoordev.backend.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import static net.doubledoordev.backend.util.Constants.*;
import static net.doubledoordev.backend.util.Constants.RANDOM;
import static net.doubledoordev.backend.util.Constants.symbols;

/**
 * Public static helper methods
 * Passed to template engine
 *
 * @author Dries007
 */
public class Helper
{
    /**
     * 1 hour
     */
    private static long CACHE_TIMEOUT = 1000 * 60 * 1;

    private Helper()
    {
    }

    /**
     * Checks to see if a port/hostname combo is available through opening a socked and closing it again
     *
     * @param hostname the hostname, if null this is bypassed
     * @param port     the port to check
     * @return true if available
     */
    @SuppressWarnings("UnusedDeclaration")
    public static boolean isPortAvailable(String hostname, int port)
    {
        try
        {
            ServerSocket socket = new ServerSocket();
            socket.bind(hostname == null || hostname.length() == 0 ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port));
            socket.close();
            return true;
        }
        catch (IOException e)
        {
            return false;
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

    private static ArrayList<String> cashedMCVersions;
    private static long lastMCVersions = 0L;

    /**
     * @return set of all MC versions we can grab from mojang
     */
    @SuppressWarnings("UnusedDeclaration")
    public static ArrayList<String> getAllMCVersions()
    {
        if (cashedMCVersions == null && System.currentTimeMillis() - lastMCVersions > CACHE_TIMEOUT)
        {
            cashedMCVersions = new ArrayList<>();
            try
            {
                JsonObject versionList = JSONPARSER.parse(IOUtils.toString(new URL(VERIONSURL).openStream())).getAsJsonObject();
                JsonObject latest = versionList.getAsJsonObject("latest");
                cashedMCVersions.add(latest.get("snapshot").getAsString());
                cashedMCVersions.add(latest.get("release").getAsString());
                for (JsonElement element : versionList.getAsJsonArray("versions"))
                {
                    JsonObject o = element.getAsJsonObject();
                    if (o.get("type").getAsString().equals("release") || o.get("type").getAsString().equals("snapshot"))
                        if (!cashedMCVersions.contains(o.get("id").getAsString()))
                            cashedMCVersions.add(o.get("id").getAsString());
                }
            }
            catch (IOException e)
            {
                //
            }
        }
        return cashedMCVersions;
    }
}
