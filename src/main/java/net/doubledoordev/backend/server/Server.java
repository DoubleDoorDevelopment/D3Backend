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

package net.doubledoordev.backend.server;

import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.commands.CommandHandler;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.query.MCQuery;
import net.doubledoordev.backend.server.query.QueryResponse;
import net.doubledoordev.backend.util.*;
import net.doubledoordev.backend.util.exceptions.AuthenticationException;
import net.doubledoordev.backend.util.exceptions.BackupException;
import net.doubledoordev.backend.util.exceptions.ServerOfflineException;
import net.doubledoordev.backend.util.exceptions.ServerOnlineException;
import net.doubledoordev.backend.util.methodCaller.IMethodCaller;
import net.doubledoordev.backend.web.socket.ServerconsoleSocketApplication;
import net.dries007.cmd.Arguments;
import net.dries007.cmd.Worker;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.*;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;

import static net.doubledoordev.backend.commands.CommandHandler.CMDCALLER;
import static net.doubledoordev.backend.util.Constants.*;

/**
 * Class that holds methods related to Server instances.
 * The json based data is in a separate class for easy GSON integration.
 *
 * @author Dries007
 */
@SuppressWarnings("UnusedDeclaration")
public class Server
{
    public static final String SERVER_PROPERTIES = "server.properties";
    public static final String SERVER_PORT = "server-port";
    public static final String QUERY_PORT = "query.port";
    public static final String QUERY_ENABLE = "enable-query";
    public static final String SERVER_IP = "server-ip";
    /**
     * Diskspace var + timer to avoid long page load times.
     */
    public int[] size = new int[3];
    public QueryResponse cachedResponse;
    public EvictingQueue<String> lastConsoleLines = EvictingQueue.create(25);
    public EvictingQueue<String> actionLog = EvictingQueue.create(50);

    @Expose
    private String ID;
    @Expose
    private Integer serverPort = 25565;
    @Expose
    private String ip = "";
    @Expose
    private String owner = "";
    @Expose
    private List<String> admins = new ArrayList<>();
    @Expose
    private List<String> coOwners = new ArrayList<>();
    @Expose
    private RestartingInfo restartingInfo = new RestartingInfo();
    @Expose
    private JvmData jvmData = new JvmData();

    /**
     * Used to reroute server output to our console.
     * NOT LOGGED TO FILE!
     */
    private Logger logger;
    private File folder;
    private File propertiesFile;
    private long propertiesFileLastModified = 0L;
    private Properties properties = new Properties();
    /**
     * MCQuery and QueryResponse instances + timer to avoid long page load times.
     */
    private MCQuery query;
    /**
     * The process the server will be running in
     */
    private Process process;
    private long startTime;
    private boolean starting = false;
    private File backupFolder;
    private WorldManager worldManager = new WorldManager(this);
    private User ownerObject;
    private boolean downloading = false;

    public Server(String ID, String owner)
    {
        this.ID = ID;
        this.owner = owner;
    }

    private Server()
    {
    }

    public void init()
    {
        if (this.logger != null) return; // don't do this twice.
        this.logger = LogManager.getLogger(ID);
        this.folder = new File(SERVERS, ID);
        this.backupFolder = new File(BACKUPS, ID);
        this.propertiesFile = new File(folder, SERVER_PROPERTIES);

        if (!backupFolder.exists()) backupFolder.mkdirs();
        if (!folder.exists()) folder.mkdir();

        getRestartingInfo().init(this);

        try
        {
            SizeCounter sizeCounter = new SizeCounter();
            Files.walkFileTree(getFolder().toPath(), sizeCounter);
            size[0] = sizeCounter.getSizeInMB();
            sizeCounter = new SizeCounter();
            if (getBackupFolder().exists()) Files.walkFileTree(getBackupFolder().toPath(), sizeCounter);
            size[1] = sizeCounter.getSizeInMB();
            size[2] = size[0] + size[1];
        }
        catch (IOException ignored)
        {
        }
        try
        {
            getProperties();
            saveProperties();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getIp()
    {
        return ip;
    }

    public boolean isDownloading()
    {
        return downloading;
    }

    public boolean isStarting()
    {
        return starting;
    }

    public MCQuery getQuery()
    {
        if (query == null) query = new MCQuery(ip != null && ip.trim().length() != 0 ? ip : LOCALHOST, serverPort);
        return query;
    }

    public Process getProcess()
    {
        return process;
    }

    /**
     * @return server.properties file
     */
    public Properties getProperties()
    {
        if (propertiesFile.exists() && propertiesFile.lastModified() > propertiesFileLastModified)
        {
            try
            {
                //properties.load(new StringReader(FileUtils.readFileToString(propertiesFile, "latin1")));
                properties.load(new StringReader(FileUtils.readFileToString(propertiesFile)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            normalizeProperties();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * Get all server.properties keys
     *
     * @return the value
     */
    public Enumeration<Object> getPropertyKeys()
    {
        return properties.keys();
    }

    public String getIP()
    {
        return ip;
    }

    /**
     * Get a server.properties
     *
     * @param key the key
     * @return the value
     */
    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }

    /**
     * Check server online status.
     * Ony detects when the process is started by us.
     * Bypass this limitation with RCon
     */
    public boolean getOnline()
    {
        if (process == null) return false;
        return process.isAlive();
    }

    /**
     * @return Human readable server address
     */
    public String getDisplayAddress()
    {
        StringBuilder builder = new StringBuilder(25);
        if (ip != null && ip.trim().length() != 0) builder.append(ip);
        else if (Strings.isNotBlank(Settings.SETTINGS.hostname)) builder.append(Settings.SETTINGS.hostname);
        builder.append(':').append(serverPort);
        return builder.toString();
    }

    public int getPid()
    {
        if (process == null) return -1;
        try
        {
            Class<?> ProcessImpl = process.getClass();
            Field field = ProcessImpl.getDeclaredField("pid");
            field.setAccessible(true);
            return field.getInt(process);
        }
        catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e)
        {
            return -1;
        }
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public int getOnlinePlayers()
    {
        return cachedResponse == null ? 0 : cachedResponse.getOnlinePlayers();
    }

    public int getSlots()
    {
        return cachedResponse == null ? -1 : cachedResponse.getMaxPlayers();
    }

    public String getMotd()
    {
        return cachedResponse == null ? "?" : cachedResponse.getMotd();
    }

    public String getGameMode()
    {
        return cachedResponse == null ? "?" : cachedResponse.getGameMode();
    }

    public String getMapName()
    {
        return cachedResponse == null ? "?" : cachedResponse.getMapName();
    }

    public ArrayList<String> getPlayerList()
    {
        return cachedResponse == null ? new ArrayList<>() : cachedResponse.getPlayerList();
    }

    public String getPlugins()
    {
        return cachedResponse == null ? "?" : cachedResponse.getPlugins();
    }

    public String getVersion()
    {
        return cachedResponse == null ? "?" : cachedResponse.getVersion();
    }

    public String getGameID()
    {
        return cachedResponse == null ? "?" : cachedResponse.getGameID();
    }

    public String getID()
    {
        return ID;
    }

    public String getOwner()
    {
        return owner;
    }

    public List<String> getAdmins()
    {
        return admins;
    }

    public User getOwnerObject()
    {
        if (ownerObject == null) ownerObject = Settings.getUserByName(getOwner());
        if (ownerObject == null)
        {
            for (User user : Settings.SETTINGS.users.values())
            {
                if (user.isAdmin())
                {
                    ownerObject = user;
                    break;
                }
            }
        }
        return ownerObject;
    }

    public List<String> getCoOwners()
    {
        return coOwners;
    }

    public File getBackupFolder()
    {
        return backupFolder;
    }

    public int[] getDiskspaceUse()
    {
        return size;
    }

    public WorldManager getWorldManager()
    {
        return worldManager;
    }

    public File getFolder()
    {
        return folder;
    }

    @Override
    public String toString()
    {
        return getID();
    }

    public RestartingInfo getRestartingInfo()
    {
        if (restartingInfo == null) restartingInfo = new RestartingInfo();
        return restartingInfo;
    }

    public JvmData getJvmData()
    {
        if (jvmData == null) jvmData = new JvmData();
        return jvmData;
    }

    public Set<String> getPossibleJarnames()
    {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.addAll(Arrays.asList(folder.list(ACCEPT_FORGE_FILTER)));
        names.addAll(Arrays.asList(folder.list(ACCEPT_MINECRAFT_SERVER_FILTER)));
        names.addAll(Arrays.asList(folder.list(ACCEPT_ALL_JAR_FILTER)));
        return names;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public void renewQuery()
    {
        try
        {
            cachedResponse = getQuery().fullStat();
        }
        catch (SocketTimeoutException ignored)
        {

        }
        catch (IOException e)
        {
            Main.LOGGER.error("Caught IOException from server {} (on port {})", ID, serverPort);
            Main.LOGGER.catching(e);
        }
    }

    private void saveProperties() throws IOException
    {
        if (!propertiesFile.exists()) propertiesFile.createNewFile();

        FileOutputStream outputStream = new FileOutputStream(propertiesFile);
        Properties properties = getProperties();
        properties.store(outputStream, "Modified by the backend");
        propertiesFileLastModified = propertiesFile.lastModified();
        outputStream.close();
    }

    public void printLine(String line)
    {
        line = Helper.stripColor(line);
        logger.info(line);
        lastConsoleLines.add(line);
        ServerconsoleSocketApplication.sendLine(this, line);
    }

    public void error(Throwable e)
    {
        logger.catching(e);
        StringWriter error = new StringWriter();
        e.printStackTrace(new PrintWriter(error));
        lastConsoleLines.add(error.toString());
        ServerconsoleSocketApplication.sendLine(this, error.toString());
    }

    public void logAction(IMethodCaller caller, String action)
    {
        if (caller == CommandHandler.CMDCALLER) return;
        printLine("[" + Constants.NAME + "] " + caller.getUser().getUsername() + ": " + action);
        actionLog.add(caller.getUser().getUsername() + ": " + action);
        // Logs to disk, otherwise it would be useless, since the printLine already logs to console.
        Main.LOGGER.info("Action on {} by {}: {}", ID, caller.getUser().getUsername(), action);
    }

    /**
     * Set a server.properties property and save the file.
     *
     * @param key   the key
     * @param value the value
     * @throws ServerOnlineException when the server is online
     */
    public void setProperty(IMethodCaller caller, String key, String value) throws IOException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Set property: " + key + " to " + value);
        properties.put(key, value);
        normalizeProperties();
        saveProperties();
    }

    public void setOwner(IMethodCaller caller, String username)
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Set owner to " + username);
        ownerObject = null;
        owner = username;
        update();
    }

    public void setServerPort(IMethodCaller caller, int serverPort) throws IOException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Set server port to " + serverPort);
        if (!Settings.SETTINGS.portRange.isInRange(serverPort)) throw new IOException("Illegal port. Must be in range: " + Settings.SETTINGS.portRange);
        this.serverPort = serverPort;
        normalizeProperties();
    }

    public void setIP(IMethodCaller caller, String IP) throws IOException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Set server ip to " + IP);
        this.ip = IP;
        normalizeProperties();
    }

    /**
     * Remove the old and download the new server jar file
     */
    public void setVersion(final IMethodCaller caller, final String version) throws BackupException
    {
        if (getOnline()) throw new ServerOnlineException();
        if (downloading) throw new IllegalStateException("Already downloading something.");
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Install Vanilla Minecraft version: " + version);
        final Server instance = this;

        new Thread(() ->
        {
            downloading = true;
            try
            {
                worldManager.doMakeAllOfTheBackup(caller);

                // delete old files
                for (File file : folder.listFiles(ACCEPT_MINECRAFT_SERVER_FILTER)) FileUtils.forceDelete(file);
                for (File file : folder.listFiles(ACCEPT_FORGE_FILTER)) FileUtils.forceDelete(file);

                File jarfile = new File(folder, getJvmData().jarName);
                if (jarfile.exists()) FileUtils.forceDelete(jarfile);
                File tempFile = new File(folder, getJvmData().jarName + ".tmp");

                // Getting new file final URL

                JsonObject versionJson;
                try
                {
                    Reader sr = new InputStreamReader(Cache.getMcVersions().get(version).openStream());
                    versionJson = Constants.JSONPARSER.parse(sr).getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("server");
                    sr.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return;
                }

                // Downloading new file

                Download download = new Download(new URL(versionJson.get("url").getAsString()), tempFile);
                download.setSize(versionJson.get("size").getAsLong());

                long lastTime = System.currentTimeMillis();
                int lastInfo = 0;

                while (download.getStatus() == Download.Status.DOWNLOADING)
                {
                    if (download.getSize() != -1)
                    {
                        caller.sendMessage(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                        printLine(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                        break;
                    }
                    Thread.sleep(100);
                }

                //methodCaller.sendProgress(0);

                while (download.getStatus() == Download.Status.DOWNLOADING)
                {
                    if ((download.getProgress() - lastInfo >= 5) || (System.currentTimeMillis() - lastTime > 1000 * 10))
                    {
                        lastInfo = (int) download.getProgress();
                        lastTime = System.currentTimeMillis();

                        //methodCaller.sendProgress(download.getProgress());
                        printLine(String.format("Downloaded %2.0f%% (%dMB / %dMB)", download.getProgress(), (download.getDownloaded() / (1024 * 1024)), (download.getSize() / (1024 * 1024))));
                    }

                    Thread.sleep(100);
                }

                if (download.getStatus() == Download.Status.ERROR)
                {
                    throw new Exception(download.getMessage());
                }
                caller.sendDone();

                tempFile.renameTo(jarfile);
                instance.update();
            }
            catch (Exception e)
            {
                error(e);
            }
            downloading = false;
        }, getID() + "-jar-downloader").start();
    }

    /**
     * Downloads and uses specific forge installer
     */
    public void installForge(final IMethodCaller caller, final String name) throws BackupException
    {
        if (getOnline()) throw new ServerOnlineException();
        final String version = Helper.getForgeVersionForName(name);
        if (version == null) throw new IllegalArgumentException("Forge with ID " + name + " not found.");
        if (downloading) throw new IllegalStateException("Already downloading something.");
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();

        logAction(caller, "Install Forge version: " + version);

        final Server instance = this;

        new Thread(() ->
        {
            downloading = true;
            try
            {
                worldManager.doMakeAllOfTheBackup(caller);

                // delete old files
                for (File file : folder.listFiles(ACCEPT_MINECRAFT_SERVER_FILTER)) FileUtils.forceDelete(file);
                for (File file : folder.listFiles(ACCEPT_FORGE_FILTER)) FileUtils.forceDelete(file);

                // download new files
                String url = Constants.FORGE_INSTALLER_URL.replace("%ID%", version);
                String forgeName = url.substring(url.lastIndexOf('/'));
                File forge = new File(folder, forgeName);
                FileUtils.copyURLToFile(new URL(url), forge);

                // run installer
                List<String> arguments = new ArrayList<>();

                arguments.add(Constants.getJavaPath());
                arguments.add("-Xmx1G");

                arguments.add("-jar");
                arguments.add(forge.getName());

                arguments.add("--installServer");

                ProcessBuilder builder = new ProcessBuilder(arguments);
                builder.directory(folder);
                builder.redirectErrorStream(true);
                final Process process = builder.start();
                printLine(arguments.toString());
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null)
                {
                    caller.sendMessage(line);
                    printLine(line);
                }

                try
                {
                    process.waitFor();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                String[] jars = folder.list(ACCEPT_FORGE_FILTER);
                if (jars != null && jars.length != 0) getJvmData().jarName = jars[0];

                FileUtils.forceDelete(forge);

                caller.sendDone();
                printLine("Forge installer done.");

                instance.update();
            }
            catch (IOException e)
            {
                printLine("##################################################################");
                printLine("Error installing a new forge version (version " + version + ")");
                printLine(e.toString());
                printLine("##################################################################");
                caller.sendError(Helper.getStackTrace(e));
                e.printStackTrace();
            }
            downloading = false;
        }, getID() + "-forge-installer").start();
    }

    public void installModpack(final IMethodCaller caller, String fileName, boolean purge, boolean twitchAppZip) throws IOException, ZipException, BackupException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        if (downloading) throw new IllegalStateException("Already downloading something.");
        final Server instance = this;

        logAction(caller, "Install Modpack: " + fileName + " Purge: " + purge + " TwitchApp: " + twitchAppZip);

        File modpackFile = new File(getFolder(), fileName);
        if (!modpackFile.exists()) throw new FileNotFoundException("File not found: " + modpackFile);
        new Thread(() ->
        {
            try
            {
                installDownloadedModpack(caller, modpackFile, purge, twitchAppZip);
            }
            catch (Exception e)
            {
                printLine("##################################################################");
                printLine("Error installing a modpack from file");
                printLine(e.toString());
                printLine("##################################################################");
                instance.error(e);
                caller.sendError(Helper.getStackTrace(e));
                e.printStackTrace();
            }
            finally
            {
                caller.sendDone();
                downloading = false;
            }
        }, getID() + "-modpack-installer").start();
    }

    public void downloadModpack(final IMethodCaller caller, String zipURL, boolean purge, boolean twitchAppZip) throws IOException, ZipException, BackupException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        if (downloading) throw new IllegalStateException("Already downloading something.");
        final Server instance = this;

        logAction(caller, "Download Modpack: " + zipURL + " Purge: " + purge + " TwitchApp: " + twitchAppZip);

        new Thread(() ->
        {
            try
            {
                final File zip = new File(folder, "modpack.zip");

                if (zip.exists()) FileUtils.forceDelete(zip);
                zip.createNewFile();

                printLine("Downloading zip...");

                Download download = new Download(Helper.getFinalURL(URLDecoder.decode(zipURL, "UTF-8")), zip);

                long lastTime = System.currentTimeMillis();
                int lastInfo = 0;

                while (download.getStatus() == Download.Status.DOWNLOADING)
                {
                    if (download.getSize() != -1)
                    {
                        caller.sendMessage(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                        printLine(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                        break;
                    }
                    Thread.sleep(100);
                }

                //caller.sendProgress(0);

                while (download.getStatus() == Download.Status.DOWNLOADING)
                {
                    if ((download.getProgress() - lastInfo >= 5) || (System.currentTimeMillis() - lastTime > 1000 * 10))
                    {
                        lastInfo = (int) download.getProgress();
                        lastTime = System.currentTimeMillis();

                        //caller.sendProgress(download.getProgress());

                        printLine(String.format("Downloaded %2.0f%% (%dMB / %dMB)", download.getProgress(), (download.getDownloaded() / (1024 * 1024)), (download.getSize() / (1024 * 1024))));
                    }

                    Thread.sleep(100);
                }

                if (download.getStatus() == Download.Status.ERROR)
                {
                    throw new Exception(download.getMessage());
                }

                printLine("Downloading zip done, extracting...");

                installDownloadedModpack(caller, zip, purge, twitchAppZip);
            }
            catch (Exception e)
            {
                printLine("##################################################################");
                printLine("Error installing a modpack from URL");
                printLine(e.toString());
                printLine("##################################################################");
                instance.error(e);
                caller.sendError(Helper.getStackTrace(e));
                e.printStackTrace();
            }
                finally
            {
                caller.sendDone();
                downloading = false;
            }
        }, getID() + "-modpack-installer").start();
    }

    private void installDownloadedModpack(final IMethodCaller methodCaller, final File zip, final boolean purge, final boolean twitchAppZip) throws Exception
    {
        downloading = true;

        worldManager.doMakeAllOfTheBackup(methodCaller);

        if (!folder.exists()) folder.mkdirs();
        if (purge)
        {
            for (File file : folder.listFiles(Constants.ACCEPT_ALL_FILTER))
            {
                if (file.equals(zip)) continue;
                FileUtils.forceDelete(file);
            }
        }

        if (twitchAppZip)
        {
            Arguments arguments = new Arguments();
            arguments.server.eula = true;
            arguments.isClient = false;
            arguments.magic = true;
            arguments.delete = true;
            arguments.input = zip.getAbsolutePath();
            arguments.output = folder;
            arguments.validate("server");

            Worker worker = new Worker(arguments);

            PipedOutputStream out = new PipedOutputStream();
            BufferedReader inReader = new BufferedReader(new InputStreamReader(new PipedInputStream(out)));
            worker.setLogger(new PrintStream(out));

            new Thread(worker, ID.concat("-curseDownloader")).start();

            while (!worker.isDone())
            {
                String line = inReader.readLine();
                if (line == null) break;
                this.printLine(line);
                methodCaller.sendMessage(line);
            }
            while (inReader.ready())
            {
                String line = inReader.readLine();
                if (line == null) break;
                this.printLine(line);
                methodCaller.sendMessage(line);
            }
            inReader.close();

            if (worker.getError() != null)
            {
                throw new Exception("Cannot run Curse Modpack downloader.", worker.getError());
            }

            String[] jars = folder.list(ACCEPT_FORGE_FILTER);
            if (jars != null && jars.length != 0) getJvmData().jarName = jars[0];
        }
        else
        {
            ZipFile zipFile = new ZipFile(zip);
            zipFile.setRunInThread(true);
            zipFile.extractAll(folder.getCanonicalPath());
            long lastTime = System.currentTimeMillis();
            int lastInfo = 0;
            while (zipFile.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
            {
                if (zipFile.getProgressMonitor().getPercentDone() - lastInfo >= 10 || System.currentTimeMillis() - lastTime > 1000 * 10)
                {
                    lastInfo = zipFile.getProgressMonitor().getPercentDone();
                    lastTime = System.currentTimeMillis();

                    printLine(String.format("Extracting %d%%", zipFile.getProgressMonitor().getPercentDone()));
                }

                Thread.sleep(10);
            }

            //methodCaller.sendProgress(100);

            FileUtils.forceDelete(zip);

            printLine("Done extracting zip.");
        }

        zip.delete();
    }

    public void removeAdmin(IMethodCaller caller, String name)
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Remove admin: " + name);
        Iterator<String> i = admins.iterator();
        while (i.hasNext())
        {
            if (i.next().equalsIgnoreCase(name)) i.remove();
        }
        update();
    }

    public void addAdmin(IMethodCaller caller, String name)
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Add admin: " + name);
        admins.add(name);
        update();
    }

    public void addCoowner(IMethodCaller caller, String name)
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Add coowner: " + name);
        coOwners.add(name);
        update();
    }

    public void removeCoowner(IMethodCaller caller, String name)
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        logAction(caller, "Remove coowner: " + name);
        Iterator<String> i = coOwners.iterator();
        while (i.hasNext())
        {
            if (i.next().equalsIgnoreCase(name)) i.remove();
        }
        update();
    }

    /**
     * Start the server in a process controlled by us.
     * Threaded to avoid haning.
     *
     * @throws ServerOnlineException
     */
    public void startServer(IMethodCaller caller) throws Exception
    {
        if (!canUserControl(caller.getUser())) throw new AuthenticationException();

        if (getOnline() || starting) throw new ServerOnlineException();
        if (downloading) throw new Exception("Still downloading something. You can see the progress in the server console.");
        if (new File(folder, getJvmData().jarName + ".tmp").exists()) throw new Exception("Minecraft server jar still downloading...");
        if (!new File(folder, getJvmData().jarName).exists()) throw new FileNotFoundException(getJvmData().jarName + " not found.");
        User user = Settings.getUserByName(getOwner());
        if (user == null) throw new Exception("No owner set??");
        if (user.getMaxRamLeft() != -1 && getJvmData().ramMax > user.getMaxRamLeft()) throw new Exception("Out of usable RAM. Lower your max RAM.");
        saveProperties();
        final Server instance = this;
        for (String blocked : SERVER_START_ARGS_BLACKLIST_PATTERNS) if (getJvmData().extraJavaParameters.contains(blocked)) throw new Exception("JVM/MC options contain a blocked option: " + blocked);
        for (String blocked : SERVER_START_ARGS_BLACKLIST_PATTERNS) if (getJvmData().extraMCParameters.contains(blocked)) throw new Exception("JVM/MC options contain a blocked option: " + blocked);

        logAction(caller, "Starting server");

        starting = true;
        File eula = new File(getFolder(), "eula.txt");
        try
        {
            FileUtils.writeStringToFile(eula, "#The server owner indicated to agree with the EULA when submitting the from that produced this server instance.\n" +
                    "#That means that there is no need for extra halting of the server startup sequence with this stupid file.\n" +
                    "#" + new Date().toString() + "\n" +
                    "eula=true\n");
        }
        catch (IOException e)
        {
            printLine("Error making the eula file....");
            e.printStackTrace();
        }

        new Thread(() ->
        {
            printLine("Starting server ................");
            try
            {
                /*
                  Build arguments list.
                 */
                List<String> arguments = new ArrayList<>();
                arguments.add(Constants.getJavaPath());
                arguments.add("-DServerID=\"" + ID + '"');
                arguments.add("-server");
                {
                    int amount = getJvmData().ramMin;
                    if (amount > 0) arguments.add(String.format("-Xms%dM", amount));
                    amount = getJvmData().ramMax;
                    if (amount > 0) arguments.add(String.format("-Xmx%dM", amount));
                }
                if (Strings.isNotBlank(getJvmData().extraJavaParameters))
                {
                    for (String arg : getJvmData().extraJavaParameters.split(" ")) arguments.add(arg);
                }
                arguments.add("-jar");
                arguments.add(getJvmData().jarName);
                arguments.add("nogui");
                if (Strings.isNotBlank(getJvmData().extraMCParameters))
                {
                    for (String arg : getJvmData().extraMCParameters.split(" ")) arguments.add(arg);
                }

                // Debug printout
                printLine("Arguments: " + arguments.toString());

                /*
                  Make ProcessBuilder, set rundir, and make sure the io gets redirected
                 */
                ProcessBuilder pb = new ProcessBuilder(arguments);
                pb.directory(folder);
                pb.redirectErrorStream(true);
                if (!new File(folder, getJvmData().jarName).exists())
                {
                    throw new FileNotFoundException("JarFile went missing in the 0.2 sec it takes for the server to start.");
                }
                process = pb.start();
                startTime = System.currentTimeMillis();
                getRestartingInfo().start();
                new Thread(() ->
                {
                    try
                    {
                        printLine("----=====##### STARTING SERVER #####=====-----");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            printLine(line);
                        }
                        printLine("----=====##### SERVER PROCESS HAS ENDED #####=====-----");
                        instance.update();
                    }
                    catch (IOException e)
                    {
                        error(e);
                    }
                    getRestartingInfo().stop();
                }, ID.concat("-streamEater")).start();
                instance.update();
            }
            catch (IOException e)
            {
                error(e);
            }
            starting = false;
        }, "ServerStarter-" + getID()).start();
    }

    /**
     * Stop the server gracefully
     */
    public boolean stopServer(IMethodCaller caller, String message)
    {
        if (!canUserControl(caller.getUser())) throw new AuthenticationException();
        if (!getOnline()) return false;

        logAction(caller, "Stopping server");

        try
        {
            sendChat(CMDCALLER, message);
            renewQuery();
            printLine("----=====##### STOPPING SERVER WITH WITH KICK #####=====-----");
            for (String user : getPlayerList()) sendCmd(CMDCALLER, String.format("kick %s %s", user, message));
            sendCmd(CMDCALLER, "stop");
            return true;
        }
        catch (Exception e)
        {
            printLine("----=====##### STOPPING SERVER #####=====-----");
            sendCmd(CMDCALLER, "stop");
            return false;
        }
    }

    public boolean forceStopServer(IMethodCaller caller) throws Exception
    {
        if (!canUserControl(caller.getUser())) throw new AuthenticationException();

        logAction(caller, "Forced stopping server");

        if (!getOnline()) throw new ServerOfflineException();
        printLine("----=====##### KILLING SERVER #####=====-----");
        process.destroy();
        return true;
    }

    public boolean murderServer(IMethodCaller caller) throws Exception
    {
        if (!canUserControl(caller.getUser())) throw new AuthenticationException();

        logAction(caller, "Murdering server");

        if (!getOnline()) throw new ServerOfflineException();
        printLine("----=====##### FORCIBLY KILLING SERVER #####=====-----");
        printLine("Begone, foul process, away with thee!");
        process.destroyForcibly();
        return true;
    }

    public boolean canUserControl(User user)
    {
        if (user == null) return false;
        if (isCoOwner(user)) return true;
        for (String admin : getAdmins()) if (admin.equalsIgnoreCase(user.getUsername())) return true;
        return false;
    }

    public boolean isCoOwner(User user)
    {
        if (user == null) return false;
        if (user.isAdmin() || user.getUsername().equalsIgnoreCase(getOwner())) return true;
        for (String admin : getCoOwners()) if (admin.equalsIgnoreCase(user.getUsername())) return true;
        return false;
    }

    public void delete(IMethodCaller caller) throws IOException
    {
        try
        {
            if (getOnline()) throw new ServerOnlineException();
            if (!caller.getUser().isAdmin() && caller.getUser() != getOwnerObject()) throw new AuthenticationException();

            logAction(caller, "Deleting server");

            Settings.SETTINGS.servers.remove(getID()); // Needs to happen first because
            FileUtils.deleteDirectory(folder);
            FileUtils.deleteDirectory(backupFolder);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void sendChat(IMethodCaller caller, String message)
    {
        if (!canUserControl(caller.getUser())) throw new AuthenticationException();

        if (!getOnline())
        {
            printLine("Server offline.");
            return;
        }

        logAction(caller, "Send chat: " + message);

        PrintWriter printWriter = new PrintWriter(process.getOutputStream());
        printWriter.print("say ");//send command /say <message>
        printWriter.println(message);
        printWriter.flush();
    }

    public void sendCmd(IMethodCaller caller, String cmd)
    {
        if (!canUserControl(caller.getUser())) throw new AuthenticationException();

        if (!getOnline())
        {
            printLine("Server offline.");
            return;
        }

        logAction(caller, "Send command: " + cmd);

        PrintWriter printWriter = new PrintWriter(process.getOutputStream());
        printWriter.println(cmd);
        printWriter.flush();
    }

    private void normalizeProperties() throws IOException
    {
        if (!properties.containsKey(SERVER_IP)) properties.setProperty(SERVER_IP, ip);
        if (!properties.containsKey(SERVER_PORT)) properties.setProperty(SERVER_PORT, String.valueOf(serverPort));
        if (!properties.containsKey(QUERY_PORT)) properties.setProperty(QUERY_PORT, String.valueOf(serverPort));

        if (Settings.SETTINGS.fixedPorts)
        {
            properties.setProperty(SERVER_PORT, String.valueOf(serverPort));
            properties.setProperty(QUERY_PORT, String.valueOf(serverPort));
        }
        else
        {
            serverPort = Integer.parseInt(properties.getProperty(SERVER_PORT, String.valueOf(serverPort)));
            if (!Settings.SETTINGS.portRange.isInRange(serverPort))
            {
                serverPort = Settings.SETTINGS.portRange.getNextAvailablePort();

            }
            properties.setProperty(QUERY_PORT, String.valueOf(serverPort));
        }

        if (Settings.SETTINGS.fixedIP) properties.setProperty(SERVER_IP, ip);
        else ip = properties.getProperty(SERVER_IP, ip);

        properties.put(QUERY_ENABLE, "true");
        query = null;
        renewQuery();
    }

    private void update()
    {
        WebSocketHelper.sendServerUpdate(this);
        Settings.save();
    }
}
