/*
 *     D3Backend
 *     Copyright (C) 2015  Dries007 & Double Door Development
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.doubledoordev.backend.util;

import com.google.common.collect.LinkedListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.server.Server;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * Contains static Runnables that are used often
 *
 * @author Dries007
 */
public class Cache extends TimerTask
{
    private static final int FORGE_MAP_CAPACITY = 2000;
    private static final LinkedHashMap<String, String> FORGE_NAME_VERSION_MAP = new LinkedHashMap<>(FORGE_MAP_CAPACITY);
    private static final Runnable FORGE_VERSIONS_DOWNLOADER = new Runnable()
    {
        private boolean hasInstaller(JsonObject object)
        {
            for (JsonElement files : object.getAsJsonArray("files"))
                for (JsonElement element : files.getAsJsonArray())
                    if (element.getAsString().equals("installer")) return true;
            return false;
        }

        class ForgeBuild
        {
            boolean hasInstaller;
            String branch;
            Integer build;
            String forgeVersion;
            String mcVersion;
            String id;

            @Override
            public String toString()
            {
                StringBuilder sb = new StringBuilder(forgeVersion).append(" (MC ").append(mcVersion).append(")");
                if (branch != null) sb.append(" [").append(branch).append(']');
                return sb.toString();
            }
        }

        @Override
        public void run()
        {
            try
            {
                Main.LOGGER.info("[Cache] Refreshing Forge version cache....");
                LinkedHashMap<String, String> orderedNameMap = new LinkedHashMap<>(FORGE_MAP_CAPACITY);
                LinkedListMultimap<String, ForgeBuild> mcForgebuildMap = LinkedListMultimap.create(FORGE_MAP_CAPACITY);
                HashMap<Integer, ForgeBuild> buildForgeMap = new HashMap<>(FORGE_MAP_CAPACITY);
                JsonObject versionList = Constants.JSONPARSER.parse(IOUtils.toString(new URL(FORGE_VERIONS_URL).openStream())).getAsJsonObject();
                JsonObject latest = versionList.getAsJsonObject("promos");

                if (!Main.running) return;

                for (Map.Entry<String, JsonElement> entry : versionList.getAsJsonObject("number").entrySet())
                {
                    JsonObject object = entry.getValue().getAsJsonObject();
                    ForgeBuild forgeBuild = new ForgeBuild();
                    forgeBuild.hasInstaller = hasInstaller(object);
                    forgeBuild.branch = object.get("branch").isJsonNull() ? null : object.get("branch").getAsString();
                    forgeBuild.build = object.get("build").getAsInt();
                    forgeBuild.forgeVersion = object.get("version").getAsString();
                    forgeBuild.mcVersion = object.get("mcversion").getAsString();
                    StringBuilder sb = new StringBuilder(forgeBuild.mcVersion).append('-').append(forgeBuild.forgeVersion);
                    if (forgeBuild.branch != null) sb.append('-').append(forgeBuild.branch);
                    forgeBuild.id = sb.toString();

                    if (forgeBuild.hasInstaller)
                    {
                        try
                        {
                            String url = Constants.FORGE_INSTALLER_URL.replace("%ID%", forgeBuild.id);
                            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                            urlConnection.setRequestMethod("GET");
                            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
                            urlConnection.connect();
                            if (urlConnection.getResponseCode() != 200) forgeBuild.hasInstaller = false;
                            urlConnection.disconnect();
                        }
                        catch (IOException ignored)
                        {
                            // timeout or something like that
                        }
                    }
                    if (!Main.running) return;
                    mcForgebuildMap.put(forgeBuild.mcVersion, forgeBuild);
                    buildForgeMap.put(forgeBuild.build, forgeBuild);
                }

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
                        orderedNameMap.put(String.format("%s (build %d)", key, latest.get(key).getAsInt()), buildForgeMap.get(latest.get(key).getAsInt()).id);
                    }
                }

                ArrayList<String> mcVersions = new ArrayList<>(mcForgebuildMap.keySet());
                Collections.reverse(mcVersions);
                for (String mcVersion : mcVersions)
                {
                    orderedNameMap.put(String.format("~~~~~~~~~~========== %s ==========~~~~~~~~~~", mcVersion), "");
                    ArrayList<ForgeBuild> forgeBuilds = new ArrayList<>(mcForgebuildMap.get(mcVersion));
                    Collections.reverse(forgeBuilds);
                    for (ForgeBuild forgeBuild : forgeBuilds)
                    {
                        if (!forgeBuild.hasInstaller) continue;
                        orderedNameMap.put(forgeBuild.toString(), forgeBuild.id);
                    }
                }

                synchronized (FORGE_NAME_VERSION_MAP)
                {
                    FORGE_NAME_VERSION_MAP.clear();
                    FORGE_NAME_VERSION_MAP.putAll(orderedNameMap);
                    FileUtils.writeStringToFile(FORGE_FILE, GSON.toJson(FORGE_NAME_VERSION_MAP));
                }
                Main.LOGGER.info("[Cache] Done refreshing Forge version cache.");
            }
            catch (IOException ignored)
            {
            }
        }
    };
    private static final ArrayList<String> CASHED_MC_VERSIONS = new ArrayList<>();
    private static final Runnable MC_VERSIONS_DOWNLOADER = new Runnable()
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
                        if (o.get("type").getAsString().equals("release") || o.get("type").getAsString().equals("snapshot")) if (!CASHED_MC_VERSIONS.contains(o.get("id").getAsString())) CASHED_MC_VERSIONS.add(o.get("id").getAsString());
                    }
                }
            }
            catch (IOException ignored)
            {
            }
        }
    };
    private static final Runnable SIZE_COUNTER = new Runnable()
    {
        @Override
        public void run()
        {
            for (Server server : Settings.SETTINGS.servers.values())
            {
                if (server.getFolder() == null || server.getBackupFolder() == null) continue;
                try
                {
                    SizeCounter sizeCounter = new SizeCounter();
                    if (server.getFolder().exists()) Files.walkFileTree(server.getFolder().toPath(), sizeCounter);
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
     * Forge version related things
     */
    private static long lastForgeVersions = 0L;
    /**
     * MC version related things
     */
    private static long lastMCVersions = 0L;
    /**
     * Size counter related things
     */
    private static long lastSize = 0L;
    /**
     *
     */
    private static boolean hasUpdate = false;
    private static String updatedVersion = "";
    private static final Runnable UPDATE_CHECKER = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                InputStream inputStream = new URL(VERSION_CHECKER_URL).openStream();
                JsonObject object = JSONPARSER.parse(new InputStreamReader(inputStream)).getAsJsonObject().getAsJsonObject("lastStableBuild");

                if (!object.get("number").getAsString().equalsIgnoreCase(Main.build))
                {
                    hasUpdate = true;
                    JsonArray artifacts = object.getAsJsonArray("artifacts");
                    for (int i = 0; i < artifacts.size(); i++)
                    {
                        Matcher matcher = Constants.VERSION_PATTERN.matcher(artifacts.get(i).getAsJsonObject().get("fileName").getAsString());
                        if (!matcher.find()) continue;
                        updatedVersion = matcher.group();
                        Main.LOGGER.warn("Version out of date! New version: " + updatedVersion);
                        break;
                    }
                }

                inputStream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    };
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

    public static boolean hasUpdate()
    {
        return hasUpdate;
    }

    public static String getUpdateVersion()
    {
        return updatedVersion;
    }

    public static void init()
    {
        if (instance != null) return;
        instance = new Cache();
        if (FORGE_FILE.exists())
        {
            try
            {
                //noinspection unchecked
                FORGE_NAME_VERSION_MAP.putAll(GSON.fromJson(FileUtils.readFileToString(FORGE_FILE), Map.class));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        TIMER.scheduleAtFixedRate(instance, 0, SHORT_CACHE_TIMEOUT);
    }

    @Override
    public void run()
    {
        long now = System.currentTimeMillis();

        if (now - lastMCVersions > REALLY_LONG_CACHE_TIMEOUT) new Thread(MC_VERSIONS_DOWNLOADER, "cache-mcVersionDownloader").start();
        if (now - lastMCVersions > REALLY_LONG_CACHE_TIMEOUT) new Thread(UPDATE_CHECKER, "cache-updateChecking").start();
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
                    try
                    {
                        server.renewQuery();
                        server.getRestartingInfo().run(server);
                    }
                    catch (Exception e)
                    {
                        Main.LOGGER.error("Exception while doing queryRenew on server: " + server.getID(), e);
                    }
                }
            }
        }, "cache-rConAndQuery").start();
    }

    public static void forceUpdateForge()
    {
        new Thread(FORGE_VERSIONS_DOWNLOADER, "forced-forgeVersionDownloader").start();
    }

    public static void forceUpdateMC()
    {
        new Thread(MC_VERSIONS_DOWNLOADER, "forced-mcVersionDownloader").start();
    }
}
