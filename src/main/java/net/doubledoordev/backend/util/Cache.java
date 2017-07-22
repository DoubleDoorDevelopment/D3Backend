/*
 * D3Backend
 * Copyright (C) 2015 - 2017  Dries007 & Double Door Development
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.doubledoordev.backend.util;

import com.google.common.collect.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.server.Server;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * Contains static TimerTasks that are used often
 *
 * @author Dries007
 */
public class Cache
{
    private static final long REALLY_LONG_CACHE_TIMEOUT = 1000 * 60 * 60 * 24; // 24 hours
    private static final long LONG_CACHE_TIMEOUT = 1000 * 60 * 60; // 1 hour
    private static final long MEDIUM_CACHE_TIMEOUT = 1000 * 60; // 1 minute
    private static final long SHORT_CACHE_TIMEOUT = 1000 * 10; // 10 seconds

    private Cache() { throw new AssertionError(); }

    /**
     * isDaemon = true, so cache won't keeps the JVM running, even if for some reason the stop function doesn't get called.
     */
    private static final Timer TIMER = new Timer("Cache-Timer", true);

    private static final Table<String, String, ForgeBuild> FORGE_VERSION_TABLE = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
    private static final TimerTask FORGE_VERSIONS_DOWNLOADER = new TimerTask()
    {
        private boolean hasInstaller(JsonObject object)
        {
            for (JsonElement files : object.getAsJsonArray("files"))
                for (JsonElement element : files.getAsJsonArray())
                    if (element.getAsString().equals("installer")) return true;
            return false;
        }

        @Override
        public void run()
        {
            try
            {
                Main.LOGGER.info("[Cache] Refreshing Forge version cache....");

                Reader reader = new InputStreamReader(new URL(FORGE_VERIONS_URL).openStream());
                JsonObject root = Constants.JSONPARSER.parse(reader).getAsJsonObject();
                reader.close();

                // WARNING: Java 8 madness ahead.
                final JsonObject number = root.getAsJsonObject("number");
                final ImmutableTable.Builder<String, String, ForgeBuild> branTheBuilder = ImmutableTable.builder();

                // MC versions
                root.getAsJsonObject("mcversion").entrySet().parallelStream() // Map<mcVersion, int[]>
                        .map(e -> new ImmutablePair<>(e.getKey(), // keep MC version intact
                                StreamSupport.stream(e.getValue().getAsJsonArray().spliterator(), true) // Stream the JsonArray
                                        .map(JsonElement::getAsString) // Map JsonElement to String (build number)
                                        .map(n -> number.get(n).getAsJsonObject()) // Map String (build number) to JsonObject from the 'number' map.
                                        .filter(this::hasInstaller) // Filter out all JsonObjects that don't have an installer listed.
                                        .map(ForgeBuild::new) // Turn JsonObject into ForgeBuild
                                        .filter(ForgeBuild::hasInstallerNetwork) // Check for Forge installer via network
                                        .sorted(Comparator.comparingInt((ForgeBuild fb) -> fb.build)) // Sort based on build number
                                        .collect(Collectors.toList()) // Turn the stream into a list.
                        )) // We have a Stream of (String:mcVersion, List<ForgeBuild>:sortedForgeBuild)
                        .filter(e -> !e.right.isEmpty()) // Filter out mc versions with no compatible Forge versions
                        .sorted(Comparator.comparing(p -> new Version(p.left))) // Sort based on MC version ID
                        .forEachOrdered(pair -> pair.right.forEach(fb -> branTheBuilder.put(pair.left, fb.id, fb)));

                // Promos
                root.getAsJsonObject("promos").entrySet().parallelStream() // Map<String, int>
                        .map(e -> new ImmutablePair<>(e.getKey(), number.get(e.getValue().getAsString()).getAsJsonObject())) // Turn int info JsonObject from 'number' map
                        .filter(e -> hasInstaller(e.right)) // Filter out all JsonObjects that don't have an installer listed.
                        .map(e -> new ImmutablePair<>(e.left, new ForgeBuild(e.right))) // Turn JsonObject into ForgeBuild
                        .filter(e -> e.right.hasInstallerNetwork()) // Filter again, based on installer via network check
                        .sorted(Comparator.comparingInt(e -> e.right.build))
                        .forEachOrdered(e -> {
                            branTheBuilder.put(e.right.mcVersion, e.left + " (" + e.right.id + ")", e.right);
                            branTheBuilder.put("Recommended/Latest", e.left + " (" + e.right.id + ")", e.right);
                        }); // Add to the 'top' of the table.

                ImmutableTable<String, String, ForgeBuild> table = branTheBuilder.build();

                if (!table.isEmpty()) {
                    FORGE_VERSION_TABLE.clear();
                    FORGE_VERSION_TABLE.putAll(table);
                }

                Main.LOGGER.info("[Cache] Done refreshing Forge version cache.");
            }
            catch (IOException ignored)
            {
            }
        }
    };
    private static final Map<String, URL> CASHED_MC_VERSIONS = new LinkedHashMap<>();
    private static final TimerTask MC_VERSIONS_DOWNLOADER = new TimerTask()
    {
        @Override
        public void run()
        {
            JsonObject versionList;
            try
            {
                Reader sr = new InputStreamReader(new URL(Constants.MC_VERIONS_URL).openStream());
                versionList = Constants.JSONPARSER.parse(sr).getAsJsonObject();
                sr.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
            synchronized (CASHED_MC_VERSIONS)
            {
                CASHED_MC_VERSIONS.clear();
                for (JsonElement element : versionList.getAsJsonArray("versions"))
                {
                    JsonObject o = element.getAsJsonObject();
                    if (!o.get("type").getAsString().equals("release") && !o.get("type").getAsString().equals("snapshot"))
                        continue;
                    try
                    {
                        CASHED_MC_VERSIONS.put(o.get("id").getAsString(), new URL(o.get("url").getAsString()));
                    }
                    catch (MalformedURLException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    private static final TimerTask SIZE_COUNTER = new TimerTask()
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
                    if (server.getBackupFolder().exists())
                        Files.walkFileTree(server.getBackupFolder().toPath(), sizeCounter);
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
    private static boolean hasUpdate = false;
    private static String updatedVersion = "";
    private static final TimerTask UPDATE_CHECKER = new TimerTask()
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

    private static final TimerTask SERVER_INFO_CHECKER = new TimerTask()
    {
        @Override
        public void run()
        {
            try
            {
                for (Server server : Settings.SETTINGS.servers.values())
                {
                    if (!server.getOnline())
                    {
                        server.cachedResponse = null;
                    }
                    else
                    {
                        try
                        {
                            server.renewQuery();
                        }
                        catch (Exception e)
                        {
                            Main.LOGGER.error("Exception while doing queryRenew on server: " + server.getID(), e);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    };

    public static void init()
    {
        if (FORGE_FILE.exists()) FORGE_FILE.delete();

        TIMER.scheduleAtFixedRate(UPDATE_CHECKER, 1000 * Constants.RANDOM.nextInt(300), REALLY_LONG_CACHE_TIMEOUT);
        TIMER.scheduleAtFixedRate(MC_VERSIONS_DOWNLOADER, 1000 * Constants.RANDOM.nextInt(300), REALLY_LONG_CACHE_TIMEOUT);

        TIMER.scheduleAtFixedRate(FORGE_VERSIONS_DOWNLOADER, 100 * Constants.RANDOM.nextInt(300), LONG_CACHE_TIMEOUT);

        TIMER.scheduleAtFixedRate(SIZE_COUNTER, 10 * Constants.RANDOM.nextInt(300), MEDIUM_CACHE_TIMEOUT);

        TIMER.scheduleAtFixedRate(SERVER_INFO_CHECKER, Constants.RANDOM.nextInt(300), SHORT_CACHE_TIMEOUT);
    }

    public static void stop()
    {
        TIMER.cancel();
        TIMER.purge();
    }

    public static void forceUpdateForge()
    {
        new Thread(FORGE_VERSIONS_DOWNLOADER, "forced-forgeVersionDownloader").start();
    }

    public static void forceUpdateMC()
    {
        new Thread(MC_VERSIONS_DOWNLOADER, "forced-mcVersionDownloader").start();
    }

    public static String getForgeVersionDownloadID(String mc, String name)
    {
        ForgeBuild fb = FORGE_VERSION_TABLE.get(mc, name);
        if (fb == null) throw new IllegalArgumentException("Illegal Forge version (" + mc + ", " + name + ")");
        return fb.networkId;
    }

    public static ImmutableMap<String, ImmutableList<String>> getForgeVersions()
    {
        return Maps.toMap(ImmutableList.copyOf(FORGE_VERSION_TABLE.rowKeySet()).reverse(), input -> ImmutableList.copyOf(FORGE_VERSION_TABLE.row(input).keySet()).reverse());
    }

    public static Map<String, URL> getMcVersions()
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

}
