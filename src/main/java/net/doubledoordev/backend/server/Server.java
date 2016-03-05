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

package net.doubledoordev.backend.server;

import com.google.gson.annotations.Expose;
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
import net.doubledoordev.cmd.CurseModpackDownloader;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;

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
    @Expose
    private final Map<String, Dimension> dimensionMap = new HashMap<>();
    /**
     * Diskspace var + timer to avoid long page load times.
     */
    public int[] size = new int[3];
    public QueryResponse cachedResponse;
    /*
     * START exposed Json data
     */
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
    /*
     * END exposed Json data
     */
    @Expose
    private RestartingInfo restartingInfo = new RestartingInfo();
    @Expose
    private JvmData jvmData = new JvmData();
    //@Expose
    //private RoastOptions roastOptions = new RoastOptions();
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

    /*
     * ========================================================================================
     * ========================================================================================
     * PUBLIC METHODS
     */

    public void init()
    {
        if (this.logger != null) return; // don't do this twice.
        this.logger = LogManager.getLogger(ID);
        this.folder = new File(SERVERS, ID);
        this.backupFolder = new File(BACKUPS, ID);
        this.propertiesFile = new File(folder, SERVER_PROPERTIES);

        if (!backupFolder.exists()) backupFolder.mkdirs();
        if (!folder.exists()) folder.mkdir();

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

    /*
     * ========================================================================================
     * GETTERS
     */

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
        if (query == null) query = new MCQuery(LOCALHOST, serverPort);
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
        normalizeProperties();
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
        try
        {
            if (process == null) return false;
            process.exitValue();
            return false;
        }
        catch (IllegalThreadStateException e)
        {
            return true;
        }
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
        return cachedResponse == null ? new ArrayList<String>() : cachedResponse.getPlayerList();
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

    public Map<String, Dimension> getDimensionMap()
    {
        return dimensionMap;
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

    public Collection<String> getPossibleJarnames()
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

    /*
     * ========================================================================================
     * SETTERS
     */

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
        properties.put(key, value);
        normalizeProperties();
        saveProperties();
    }

    public void setOwner(IMethodCaller methodCaller, String username)
    {
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        ownerObject = null;
        owner = username;
        update();
    }

    public void setServerPort(IMethodCaller caller, int serverPort) throws IOException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        this.serverPort = serverPort;
        normalizeProperties();
    }

    public void setIP(IMethodCaller caller, String IP) throws IOException
    {
        if (!isCoOwner(caller.getUser())) throw new AuthenticationException();
        this.ip = IP;
        normalizeProperties();
    }

    /*
     * ========================================================================================
     * SETTERS VIA DOWNLOADERS
     */

    /**
     * Remove the old and download the new server jar file
     */
    public void setVersion(final IMethodCaller methodCaller, final String version) throws BackupException
    {
        if (getOnline()) throw new ServerOnlineException();
        if (downloading) throw new IllegalStateException("Already downloading something.");
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        final Server instance = this;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                downloading = true;
                try
                {
                    worldManager.doMakeAllOfTheBackup(methodCaller);

                    // delete old files
                    for (File file : folder.listFiles(ACCEPT_MINECRAFT_SERVER_FILTER)) FileUtils.forceDelete(file);
                    for (File file : folder.listFiles(ACCEPT_FORGE_FILTER)) FileUtils.forceDelete(file);

                    File jarfile = new File(folder, getJvmData().jarName);
                    if (jarfile.exists()) FileUtils.forceDelete(jarfile);
                    File tempFile = new File(folder, getJvmData().jarName + ".tmp");

                    // Downloading new file

                    Download download = new Download(new URL(Constants.MC_SERVER_JAR_URL.replace("%ID%", version)), tempFile);

                    long lastTime = System.currentTimeMillis();
                    int lastInfo = 0;

                    while (download.getStatus() == Download.Status.Downloading)
                    {
                        if (download.getSize() != -1)
                        {
                            methodCaller.sendMessage(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                            printLine(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                            break;
                        }
                        Thread.sleep(10);
                    }

                    //methodCaller.sendProgress(0);

                    while (download.getStatus() == Download.Status.Downloading)
                    {
                        if ((download.getProgress() - lastInfo >= 5) || (System.currentTimeMillis() - lastTime > 1000 * 10))
                        {
                            lastInfo = (int) download.getProgress();
                            lastTime = System.currentTimeMillis();

                            //methodCaller.sendProgress(download.getProgress());
                            printLine(String.format("Downloaded %2.0f%% (%dMB / %dMB)", download.getProgress(), (download.getDownloaded() / (1024 * 1024)), (download.getSize() / (1024 * 1024))));
                        }

                        Thread.sleep(10);
                    }

                    if (download.getStatus() == Download.Status.Error)
                    {
                        throw new Exception(download.getMessage());
                    }
                    methodCaller.sendDone();

                    tempFile.renameTo(jarfile);
                    instance.update();
                }
                catch (Exception e)
                {
                    error(e);
                }
                downloading = false;
            }
        }, getID() + "-jar-downloader").start();
    }

    /**
     * Downloads and uses specific forge installer
     */
    public void installForge(final IMethodCaller methodCaller, final String name) throws BackupException
    {
        if (getOnline()) throw new ServerOnlineException();
        final String version = Helper.getForgeVersionForName(name);
        if (version == null) throw new IllegalArgumentException("Forge with ID " + name + " not found.");
        if (downloading) throw new IllegalStateException("Already downloading something.");
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        final Server instance = this;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                downloading = true;
                try
                {
                    worldManager.doMakeAllOfTheBackup(methodCaller);

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
                        methodCaller.sendMessage(line);
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

                    for (String name : folder.list(ACCEPT_FORGE_FILTER)) getJvmData().jarName = name;

                    FileUtils.forceDelete(forge);

                    methodCaller.sendDone();
                    printLine("Forge installer done.");

                    instance.update();
                }
                catch (IOException e)
                {
                    printLine("##################################################################");
                    printLine("Error installing a new forge version (version " + version + ")");
                    printLine(e.toString());
                    printLine("##################################################################");
                    methodCaller.sendError(Helper.getStackTrace(e));
                    e.printStackTrace();
                }
                downloading = false;
            }
        }, getID() + "-forge-installer").start();
    }

    public void downloadModpack(final IMethodCaller methodCaller, final String zipURL, final boolean purge, final boolean cuseZip) throws IOException, ZipException, BackupException
    {
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        if (downloading) throw new IllegalStateException("Already downloading something.");
        final Server instance = this;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    downloading = true;

                    worldManager.doMakeAllOfTheBackup(methodCaller);

                    if (purge)
                    {
                        for (File file : folder.listFiles(Constants.ACCEPT_ALL_FILTER))
                        {
                            FileUtils.forceDelete(file);
                        }
                    }
                    if (!folder.exists()) folder.mkdirs();

                    final File zip = new File(folder, "modpack.zip");

                    if (zip.exists()) FileUtils.forceDelete(zip);
                    zip.createNewFile();

                    printLine("Downloading zip...");

                    Download download = new Download(new URL(URLDecoder.decode(zipURL, "UTF-8")), zip);

                    long lastTime = System.currentTimeMillis();
                    int lastInfo = 0;

                    while (download.getStatus() == Download.Status.Downloading)
                    {
                        if (download.getSize() != -1)
                        {
                            methodCaller.sendMessage(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                            printLine(String.format("Download is %dMB", (download.getSize() / (1024 * 1024))));
                            break;
                        }
                        Thread.sleep(10);
                    }

                    //methodCaller.sendProgress(0);

                    while (download.getStatus() == Download.Status.Downloading)
                    {
                        if ((download.getProgress() - lastInfo >= 5) || (System.currentTimeMillis() - lastTime > 1000 * 10))
                        {
                            lastInfo = (int) download.getProgress();
                            lastTime = System.currentTimeMillis();

                            //methodCaller.sendProgress(download.getProgress());

                            printLine(String.format("Downloaded %2.0f%% (%dMB / %dMB)", download.getProgress(), (download.getDownloaded() / (1024 * 1024)), (download.getSize() / (1024 * 1024))));
                        }

                        Thread.sleep(10);
                    }

                    if (download.getStatus() == Download.Status.Error)
                    {
                        throw new Exception(download.getMessage());
                    }

                    if (cuseZip)
                    {
                        CurseModpackDownloader curseModpackDownloader = new CurseModpackDownloader();
                        curseModpackDownloader.eula = true;
                        curseModpackDownloader.server = true;
                        curseModpackDownloader.input = zip;
                        curseModpackDownloader.output = folder;
                        curseModpackDownloader.ignoreExistingMods = true;
                        curseModpackDownloader.logger = new OutWrapper(System.out, methodCaller, instance);

                        curseModpackDownloader.run();

                        FileUtils.forceDelete(zip);

                        printLine("Done extracting & installing zip.");
                    }
                    else
                    {
                        printLine("Downloading zip done, extracting...");

                        ZipFile zipFile = new ZipFile(zip);
                        zipFile.setRunInThread(true);
                        zipFile.extractAll(folder.getCanonicalPath());
                        lastTime = System.currentTimeMillis();
                        lastInfo = 0;
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

                    methodCaller.sendDone();
                }
                catch (Exception e)
                {
                    printLine("##################################################################");
                    printLine("Error installing a modpack");
                    printLine(e.toString());
                    printLine("##################################################################");
                    methodCaller.sendError(Helper.getStackTrace(e));
                    e.printStackTrace();
                }
                downloading = false;
            }
        }, getID() + "-modpack-installer").start();
    }

    /*
     * ========================================================================================
     * REMOVE and ADD methods
     */

    public void removeAdmin(IMethodCaller methodCaller, String name)
    {
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        Iterator<String> i = admins.iterator();
        while (i.hasNext())
        {
            if (i.next().equalsIgnoreCase(name)) i.remove();
        }
        update();
    }

    public void addAdmin(IMethodCaller methodCaller, String name)
    {
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        admins.add(name);
        update();
    }

    public void addCoowner(IMethodCaller methodCaller, String name)
    {
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        coOwners.add(name);
        update();
    }

    public void removeCoowner(IMethodCaller methodCaller, String name)
    {
        if (!isCoOwner(methodCaller.getUser())) throw new AuthenticationException();
        Iterator<String> i = coOwners.iterator();
        while (i.hasNext())
        {
            if (i.next().equalsIgnoreCase(name)) i.remove();
        }
        update();
    }

    /*
     * ========================================================================================
     * MAKE or RENEW methods
     */
    public void renewQuery()
    {
        cachedResponse = getQuery().fullStat();
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

    /**
     * Start the server in a process controlled by us.
     * Threaded to avoid haning.
     *
     * @throws ServerOnlineException
     */
    public void startServer() throws Exception
    {
        if (getOnline() || starting) throw new ServerOnlineException();
        if (downloading) throw new Exception("Still downloading something. You can see the progress in the server console.");
        if (new File(folder, getJvmData().jarName + ".tmp").exists()) throw new Exception("Minecraft server jar still downloading...");
        if (!new File(folder, getJvmData().jarName).exists()) throw new FileNotFoundException(getJvmData().jarName + " not found.");
        User user = Settings.getUserByName(getOwner());
        if (user == null) throw new Exception("No owner set??");
        if (user.getMaxRamLeft() != -1 && getJvmData().ramMax > user.getMaxRamLeft()) throw new Exception("Out of usable RAM. Lower your max RAM.");
        saveProperties();
        starting = true;
        final Server instance = this;
        for (String blocked : SERVER_START_ARGS_BLACKLIST_PATTERNS) if (getJvmData().extraJavaParameters.contains(blocked)) throw new Exception("JVM/MC options contain a blocked option: " + blocked);
        for (String blocked : SERVER_START_ARGS_BLACKLIST_PATTERNS) if (getJvmData().extraMCParameters.contains(blocked)) throw new Exception("JVM/MC options contain a blocked option: " + blocked);

        File eula = new File(getFolder(), "eula.txt");
        if (!eula.exists())
        {
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
        }

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                printLine("Starting server ................");
                try
                {
                    /**
                     * Build arguments list.
                     */
                    List<String> arguments = new ArrayList<>();
                    arguments.add(Constants.getJavaPath());
                    arguments.add("-server");
                    {
                        int amount = getJvmData().ramMin;
                        if (amount > 0) arguments.add(String.format("-Xms%dM", amount));
                        amount = getJvmData().ramMax;
                        if (amount > 0) arguments.add(String.format("-Xmx%dM", amount));
                        amount = getJvmData().permGen;
                        if (amount > 0) arguments.add(String.format("-XX:MaxPermSize=%dm", amount));
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

                    /**
                     * Make ProcessBuilder, set rundir, and make sure the io gets redirected
                     */
                    ProcessBuilder pb = new ProcessBuilder(arguments);
                    pb.directory(folder);
                    pb.redirectErrorStream(true);
                    if (!new File(folder, getJvmData().jarName).exists()) return; // for reasons of WTF?
                    process = pb.start();
                    startTime = System.currentTimeMillis();
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
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
                        }
                    }, ID.concat("-streamEater")).start();
                    instance.update();
                }
                catch (IOException e)
                {
                    error(e);
                }
                starting = false;
            }
        }, "ServerStarter-" + getID()).start(); // <-- Very important call.
    }

    public void printLine(String line)
    {
        logger.info(line);
        ServerconsoleSocketApplication.sendLine(this, line);
    }

    public void error(Throwable e)
    {
        logger.catching(e);
        StringWriter error = new StringWriter();
        e.printStackTrace(new PrintWriter(error));
        ServerconsoleSocketApplication.sendLine(this, error.toString());
    }

    /**
     * Stop the server gracefully
     */
    public boolean stopServer(String message)
    {
        if (!getOnline()) return false;
        try
        {
            sendChat(message);
            renewQuery();
            printLine("----=====##### STOPPING SERVER WITH WITH KICK #####=====-----");
            for (String user : getPlayerList()) sendCmd(String.format("kick %s %s", user, message));
            sendCmd("stop");
            return true;
        }
        catch (Exception e)
        {
            printLine("----=====##### STOPPING SERVER #####=====-----");
            sendCmd("stop");
            return false;
        }
    }

    public boolean forceStopServer() throws Exception
    {
        if (!getOnline()) throw new ServerOfflineException();
        printLine("----=====##### KILLING SERVER #####=====-----");
        process.destroy();
        try
        {
            process.getOutputStream().close();
        }
        catch (IOException ignored)
        {
        }
        try
        {
            process.getErrorStream().close();
        }
        catch (IOException ignored)
        {
        }
        try
        {
            process.getInputStream().close();
        }
        catch (IOException ignored)
        {
        }
        return true;
    }

    public boolean canUserControl(User user)
    {
        if (user == null) return false;
        if (user.isAdmin() || user.getUsername().equalsIgnoreCase(getOwner())) return true;
        for (String admin : getAdmins()) if (admin.equalsIgnoreCase(user.getUsername())) return true;
        for (String admin : getCoOwners()) if (admin.equalsIgnoreCase(user.getUsername())) return true;
        return false;
    }

    public boolean isCoOwner(User user)
    {
        if (user == null) return false;
        if (user.isAdmin() || user.getUsername().equalsIgnoreCase(getOwner())) return true;
        for (String admin : getCoOwners()) if (admin.equalsIgnoreCase(user.getUsername())) return true;
        return false;
    }

    public void delete(final IMethodCaller methodCaller) throws IOException
    {
        try
        {
            if (getOnline()) throw new ServerOnlineException();
            if (!methodCaller.getUser().isAdmin() && methodCaller.getUser() != getOwnerObject()) throw new AuthenticationException();
            Settings.SETTINGS.servers.remove(getID()); // Needs to happen first because
            FileUtils.deleteDirectory(folder);
            FileUtils.deleteDirectory(backupFolder);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void sendChat(String message)
    {
        PrintWriter printWriter = new PrintWriter(process.getOutputStream());
        printWriter.print("say ");//send command /say <message>
        printWriter.println(message);
        printWriter.flush();
    }

    public void sendCmd(String s)
    {
        PrintWriter printWriter = new PrintWriter(process.getOutputStream());
        printWriter.println(s);
        printWriter.flush();
    }

    /*
     * ========================================================================================
     * ========================================================================================
     * PRIVATE METHODS
     */

    private void normalizeProperties()
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
