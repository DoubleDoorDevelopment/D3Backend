/*
 * Unless otherwise specified through the '@author' tag or comments at
 * the top of the file or on a specific portion of the code the following license applies:
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
 *  The header specified or the above copyright notice, this list of conditions
 *   and the following disclaimer below must be displayed at the top of the source code
 *   of any web page received while using any part of the service this software provides.
 *
 *   The header to be displayed:
 *       This page was generated by DoubleDoorDevelopment's D3Backend or a derivative thereof.
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.webserver.Webserver;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

import static net.doubledoordev.backend.util.Constants.FORGE_VERIONS_URL;
import static net.doubledoordev.backend.util.Constants.TIMER;

/**
 * Contains static Runnables that are used often
 *
 * @author Dries007
 */
public class Cache extends TimerTask
{
    private static final int                           FORGE_MAP_CAPACITY        = 2000;
    private static final LinkedHashMap<String, String> FORGE_NAME_VERSION_MAP    = new LinkedHashMap<>(FORGE_MAP_CAPACITY);
    private static final Runnable                      FORGE_VERSIONS_DOWNLOADER = new Runnable()
    {
        private boolean hasInstaller(JsonObject object)
        {
            for (JsonElement files : object.getAsJsonArray("files"))
                for (JsonElement element : files.getAsJsonArray())
                    if (element.getAsString().equals("installer"))
                        return true;
            return false;
        }

        @Override
        public void run()
        {
            try
            {
                Main.LOGGER.info("[Cache] Refreshing Forge version cache....");
                LinkedHashMap<String, Integer> nameBuildMap = new LinkedHashMap<>(FORGE_MAP_CAPACITY);
                HashMap<Integer, String> buildVersionMap = new HashMap<>(FORGE_MAP_CAPACITY);
                JsonObject versionList = Constants.JSONPARSER.parse(IOUtils.toString(new URL(FORGE_VERIONS_URL).openStream())).getAsJsonObject();
                JsonObject latest = versionList.getAsJsonObject("promos");
                HashSet<Integer> buildsWithoutInstaller = new HashSet<>();

                {
                    LinkedList<String> list = new LinkedList<>();
                    for (Map.Entry<String, JsonElement> element : latest.entrySet())
                    {
                        list.add(element.getKey());
                    }
                    Iterator<String> i = list.descendingIterator();
                    while (i.hasNext())
                    {
                        String key = i.next();
                        nameBuildMap.put(String.format("%s (build %d)", key, latest.get(key).getAsInt()), latest.get(key).getAsInt());
                    }
                }

                String lastMc = "";
                ArrayList<Map.Entry<String, JsonElement>> entries = new ArrayList<>(versionList.getAsJsonObject("number").entrySet());
                for (int i = entries.size() - 1; i > 0; i--)
                {
                    JsonObject object = entries.get(i).getValue().getAsJsonObject();
                    String mc = object.get("mcversion").getAsString();
                    String version = object.get("version").getAsString();
                    int build = object.get("build").getAsInt();

                    if (!lastMc.equals(mc) && hasInstaller(object))
                    {
                        nameBuildMap.put(String.format("~~~~~~~~~~========== %s ==========~~~~~~~~~~", mc), 0);
                        lastMc = mc;
                    }

                    nameBuildMap.put(String.format("%s (MC %s)", version, mc), build);
                    buildVersionMap.put(build, String.format("%s-%s", mc, version));

                    if (!hasInstaller(object)) buildsWithoutInstaller.add(build);
                    else
                    {
                        try
                        {
                            String url = Constants.FORGE_INSTALLER_URL.replace("%ID%", String.format("%s-%s", mc, version));
                            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                            urlConnection.setRequestMethod("GET");
                            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
                            urlConnection.connect();
                            if (urlConnection.getResponseCode() != 200) buildsWithoutInstaller.add(build);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                Main.LOGGER.debug("[Cache] Excluded FORGE versions: " + buildsWithoutInstaller.toString());

                synchronized (FORGE_NAME_VERSION_MAP)
                {
                    FORGE_NAME_VERSION_MAP.clear();
                    for (Map.Entry<String, Integer> entry : nameBuildMap.entrySet())
                    {
                        if (!buildsWithoutInstaller.contains(entry.getValue())) FORGE_NAME_VERSION_MAP.put(entry.getKey(), buildVersionMap.get(entry.getValue()));
                    }
                }
                Main.LOGGER.info("[Cache] Done refreshing Forge version cache.");
            }
            catch (IOException ignored)
            {
            }
        }
    };
    private static final ArrayList<String>             CASHED_MC_VERSIONS        = new ArrayList<>();
    private static final Runnable                      MC_VERSIONS_DOWNLOADER    = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                JsonObject versionList = Constants.JSONPARSER.parse(IOUtils.toString(new URL(Constants.MC_VERIONS_URL).openStream())).getAsJsonObject();
                JsonObject latest = versionList.getAsJsonObject("latest");
                synchronized (CASHED_MC_VERSIONS)
                {
                    CASHED_MC_VERSIONS.clear();
                    CASHED_MC_VERSIONS.add(latest.get("snapshot").getAsString());
                    CASHED_MC_VERSIONS.add(latest.get("release").getAsString());
                    for (JsonElement element : versionList.getAsJsonArray("versions"))
                    {
                        JsonObject o = element.getAsJsonObject();
                        if (o.get("type").getAsString().equals("release") || o.get("type").getAsString().equals("snapshot"))
                            if (!CASHED_MC_VERSIONS.contains(o.get("id").getAsString()))
                                CASHED_MC_VERSIONS.add(o.get("id").getAsString());
                    }
                }
            }
            catch (IOException ignored)
            {
            }
        }
    };
    private static final Runnable                      SIZE_COUNTER              = new Runnable()
    {
        @Override
        public void run()
        {
            for (Server server : Settings.SETTINGS.servers.values())
            {
                try
                {
                    SizeCounter sizeCounter = new SizeCounter();
                    Files.walkFileTree(server.getFolder().toPath(), sizeCounter);
                    server.size[0] = sizeCounter.getSizeInMB();
                    sizeCounter = new SizeCounter();
                    if (server.getBackupFolder().exists()) Files.walkFileTree(server.getBackupFolder().toPath(), sizeCounter);
                    server.size[1] = sizeCounter.getSizeInMB();
                    server.size[2] = server.size[0] + server.size[1];
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    };
    /**
     * Time vars
     */
    public static        long                          LONG_CACHE_TIMEOUT        = 1000 * 60 * 60;   // 1 hour
    public static        long                          MEDIUM_CACHE_TIMEOUT      = 1000 * 60;       // 1 minute
    public static        long                          SHORT_CACHE_TIMEOUT       = 1000 * 10;       // 20 seconds
    /**
     * Forge version related things
     */
    private static       long                          lastForgeVersions         = 0L;
    /**
     * MC version related things
     */
    private static       long                          lastMCVersions            = 0L;
    /**
     * Size counter related things
     */
    private static       long                          lastSize                  = 0L;
    /**
     * Timer related things
     */
    private static Cache instance;

    private Cache()
    {

    }

    public static Collection<String> getForgeNames()
    {
        return FORGE_NAME_VERSION_MAP.keySet();
    }

    public static String getForgeVersionForName(String name)
    {
        return FORGE_NAME_VERSION_MAP.get(name);
    }

    public static Collection<String> getMcVersions()
    {
        return CASHED_MC_VERSIONS;
    }

    public static void init()
    {
        if (instance != null) return;
        instance = new Cache();
        TIMER.scheduleAtFixedRate(instance, 0, SHORT_CACHE_TIMEOUT);
    }

    @Override
    public void run()
    {
        long now = System.currentTimeMillis();

        if (now - Webserver.WEBSERVER.lastRequest > MEDIUM_CACHE_TIMEOUT) return;

        if (now - lastMCVersions > LONG_CACHE_TIMEOUT) new Thread(MC_VERSIONS_DOWNLOADER, "cache-mcVersionDownloader").start();
        if (now - lastForgeVersions > LONG_CACHE_TIMEOUT) new Thread(FORGE_VERSIONS_DOWNLOADER, "cache-forgeVersionDownloader").start();
        if (now - lastSize > MEDIUM_CACHE_TIMEOUT) new Thread(SIZE_COUNTER, "cache-sizeCounter").start();

        lastMCVersions = lastForgeVersions = lastSize = now;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for (Server server : Settings.SETTINGS.servers.values())
                {
                    server.renewQuery();
                    if (server.getRCon() == null) server.makeRcon();
                }
            }
        }, "cache-rConAndQuery").start();
    }
}
