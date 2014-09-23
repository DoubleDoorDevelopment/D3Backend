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

package net.doubledoordev.backend.server;

import com.google.gson.*;
import net.doubledoordev.backend.permissions.Group;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.query.MCQuery;
import net.doubledoordev.backend.server.query.QueryResponse;
import net.doubledoordev.backend.server.rcon.RCon;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.exceptions.ServerOfflineException;
import net.doubledoordev.backend.util.exceptions.ServerOnlineException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * Class that holds methods related to Server instances.
 * The json based data is in a separate class for easy GSON integration.
 *
 * @author Dries007
 */
public class Server
{
    private static final String SERVER_PROPERTIES = "server.properties";
    private static final String SERVER_PORT = "server-port";
    private static final String QUERY_PORT = "query.port";
    private static final String QUERY_ENABLE = "enable-query";
    private static final String RCON_ENABLE = "enable-rcon";
    private static final String RCON_PASSWORD = "rcon.password";
    private static final String RCON_PORT = "rcon.port";
    private static final String SERVER_IP = "server-ip";
    /**
     * 10 seconds
     */
    private static final int CACHE_TIMEOUT = 1000 * 10;

    /**
     * Java bean holding all the config data
     */
    private final ServerData data;

    /**
     * Used to reroute server output to our console.
     * NOT LOGGED TO FILE!
     */
    private Logger logger;
    private File folder;
    private File propertiesFile;
    private boolean propertiesLoaded = false;
    private Properties properties = new Properties();

    /**
     * RCon instance + timer to avoid long page load times.
     */
    private RCon rCon;
    private long lastRcon = 0L;

    /**
     * MCQuery and QueryResponse instances + timer to avoid long page load times.
     */
    private MCQuery query;
    private long lastQuery = 0L;
    private QueryResponse cachedResponse;

    /**
     * The process the server will be running in
     */
    private Process process;
    private boolean starting = false;
    private final LinkedList<String> last25LogLines = new LinkedList<>();

    public Server(ServerData data)
    {
        this.data = data;
        this.logger = LogManager.getLogger(data.name);
        this.folder = new File(SERVERS, data.name);
        this.propertiesFile = new File(folder, SERVER_PROPERTIES);

        if (!folder.exists()) folder.mkdir();
        normalizeProperties();

        // Check to see if the server is running outside the backend, if so reboot please!
        if (getRCon() != null)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        RCon rCon = getRCon();
                        for (String user : getPlayerList())
                            rCon.send("kick", user, NAME + " is taking over! Server Reboot!");
                        rCon.stop();
                        startServer();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        // Handle autostart
        if (!getOnline() && getAutoStart())
        {
            try
            {
                startServer();
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    /**
     * Proper way of obtaining a RCon instance
     *
     * @return null if offine!
     */
    public RCon getRCon()
    {
        if (rCon == null && System.currentTimeMillis() - lastRcon > CACHE_TIMEOUT)
        {
            lastRcon = System.currentTimeMillis();
            try
            {
                rCon = new RCon(LOCALHOST, data.rconPort, data.rconPswd.toCharArray());
            }
            catch (Exception e)
            {
                // Server offline.
            }
        }
        return rCon;
    }

    /**
     * Proper way of obtaining a MCQuery instance
     */
    public MCQuery getQuery()
    {
        if (query == null) query = new MCQuery(LOCALHOST, data.serverPort);
        return query;
    }

    /**
     * The properties from the server.properties file
     * Reloads form file!
     */
    public Properties getProperties()
    {
        propertiesLoaded = true;
        if (propertiesFile.exists())
        {
            try
            {
                FileReader fileReader = new FileReader(propertiesFile);
                logger.debug("Reading properties with encoding: " + fileReader.getEncoding());
                properties.load(fileReader);
                fileReader.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return properties;
    }

    /**
     * Saves the server.properties
     */
    public void saveProperties()
    {
        if (!propertiesLoaded) getProperties();

        normalizeProperties();

        try
        {
            if (!propertiesFile.exists()) //noinspection ResultOfMethodCallIgnored
                propertiesFile.createNewFile();
            FileWriter fileWriter = new FileWriter(propertiesFile);
            logger.debug("Saving properties with encoding: " + fileWriter.getEncoding());
            properties.store(fileWriter, "Minecraft server properties\nModified by D3Backend");
            fileWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void normalizeProperties()
    {
        if (!Settings.SETTINGS.fixedPorts)
        {
            properties.setProperty(SERVER_PORT, String.valueOf(data.serverPort));
            properties.setProperty(QUERY_PORT, String.valueOf(data.serverPort));
        }
        else
        {
            data.serverPort = Integer.parseInt(properties.getProperty(SERVER_PORT, String.valueOf(data.serverPort)));
            properties.setProperty(QUERY_PORT, String.valueOf(data.serverPort));
        }

        if (!Settings.SETTINGS.fixedIP) properties.setProperty(SERVER_IP, data.ip);
        else data.ip = properties.getProperty(SERVER_IP, data.ip);

        if (!Settings.SETTINGS.fixedPorts) properties.setProperty(RCON_PORT, String.valueOf(data.rconPort));
        else data.rconPort = Integer.parseInt(properties.getProperty(RCON_PORT, String.valueOf(data.rconPort)));

        properties.put(RCON_ENABLE, "true");
        properties.put(QUERY_ENABLE, "true");
        properties.put(RCON_PASSWORD, data.rconPswd);
    }

    /**
     * Check to see if the cachedResponse is still valid.
     * If not, its automatically renewed.
     */
    private void checkCachedResponse()
    {
        if (System.currentTimeMillis() - lastQuery > CACHE_TIMEOUT)
        {
            cachedResponse = getQuery().fullStat();
            lastQuery = System.currentTimeMillis();
        }
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
     * Invokes getter method with reflection.
     * Used in templates as 'get($key)' where $key can be assigned by a list.
     *
     * @param name of property
     * @return null or the result of the method
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Object get(String name) throws InvocationTargetException, IllegalAccessException
    {
        for (Method method : this.getClass().getDeclaredMethods())
            if (method.getName().equalsIgnoreCase("get" + name)) return method.invoke(this);
        return null;
    }

    /**
     * @return Human readable server address
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getDisplayAddress()
    {
        StringBuilder builder = new StringBuilder(25);
        if (data.ip != null && data.ip.trim().length() != 0) builder.append(data.ip);
        else builder.append(Settings.SETTINGS.hostname);
        builder.append(':').append(data.serverPort);
        return builder.toString();
    }

    /**
     * Set a server.properties property and save the file.
     *
     * @param key   the key
     * @param value the value
     * @throws ServerOnlineException when the server is online
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setProperty(String key, String value) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        properties.put(key, value);
        normalizeProperties();
    }

    /**
     * Get a server.properties
     *
     * @param key the key
     * @return the value
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }

    /**
     * Get all server.properties keys
     *
     * @return the value
     */
    @SuppressWarnings("UnusedDeclaration")
    public Enumeration<Object> getPropertyKeys()
    {
        return properties.keys();
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getServerPort()
    {
        return Integer.parseInt(getProperty(SERVER_PORT));
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getRconPort()
    {
        return Integer.parseInt(getProperty(RCON_PORT));
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getOnlinePlayers()
    {
        checkCachedResponse();
        return cachedResponse == null ? -1 : cachedResponse.getOnlinePlayers();
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getSlots()
    {
        checkCachedResponse();
        return cachedResponse == null ? -1 : cachedResponse.getMaxPlayers();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getMotd()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getMotd();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getGameMode()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getGameMode();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getMapName()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getMapName();
    }

    @SuppressWarnings("UnusedDeclaration")
    public ArrayList<String> getPlayerList()
    {
        checkCachedResponse();
        return cachedResponse == null ? new ArrayList<String>() : cachedResponse.getPlayerList();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getPlugins()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getPlugins();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getVersion()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getVersion();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getGameID()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getGameID();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getName()
    {
        return data.name;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getRamMin()
    {
        return data.ramMin;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRamMin(int ramMin) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.ramMin = ramMin;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getRamMax()
    {
        return data.ramMax;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRamMax(int ramMax) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.ramMax = ramMax;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getPermGen()
    {
        return data.permGen;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPermGen(int permGen) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.permGen = permGen;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public List<String> getExtraJavaParameters()
    {
        return data.extraJavaParameters;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExtraJavaParameters(String list) throws Exception
    {
        setExtraJavaParameters(Arrays.asList(list.split(",")));
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExtraJavaParameters(List<String> list) throws Exception
    {
        if (getOnline()) throw new ServerOnlineException();
        for (String s : list)
            for (Pattern pattern : Constants.ILLEGAL_OPTIONS)
                if (pattern.matcher(s).matches()) throw new Exception(s + " NOT ALLOWED.");
        data.extraJavaParameters = list;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public List<String> getExtraMCParameters()
    {
        return data.extraMCParameters;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExtraMCParameters(String list) throws Exception
    {
        setExtraMCParameters(Arrays.asList(list.split(",")));
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExtraMCParameters(List<String> list) throws Exception
    {
        if (getOnline()) throw new ServerOnlineException();
        for (String s : list)
            for (Pattern pattern : Constants.ILLEGAL_OPTIONS)
                if (pattern.matcher(s).matches()) throw new Exception(s + " NOT ALLOWED.");
        data.extraMCParameters = list;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getJarName()
    {
        return data.jarName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setJarName(String jarName) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.jarName = jarName;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean getAutoStart()
    {
        return data.autoStart;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setAutoStart(boolean autoStart)
    {
        logger.error("setAutoStart " + autoStart);
        data.autoStart = autoStart;
        Settings.save();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getOwner()
    {
        return data.owner;
    }

    @SuppressWarnings("UnusedDeclaration")
    public List<String> getAdmins()
    {
        return data.admins;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getPropertiesAsText()
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            properties.store(stringWriter, "Edited via " + NAME);
        }
        catch (IOException e)
        {
            // e.printStackTrace();
        }
        return stringWriter.toString();
    }

    public void setPropertiesAsText(String urlEncodedText)
    {
        try
        {
            properties.clear();
            properties.load(new StringReader(urlEncodedText));
            saveProperties();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Clear the extraJavaParameters array.
     *
     * @throws ServerOnlineException
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setExtraJavaParameters() throws Exception
    {
        setExtraJavaParameters(Arrays.asList(new String[0]));
    }

    /**
     * Clear the extraMCParameters array.
     *
     * @throws ServerOnlineException
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setExtraMCParameters() throws Exception
    {
        setExtraMCParameters(Arrays.asList(new String[0]));
    }

    /**
     * Start the server in a process controlled by us.
     * Threaded to avoid haning.
     *
     * @throws ServerOnlineException
     */
    @SuppressWarnings("UnusedDeclaration")
    public void startServer() throws Exception
    {
        if (getOnline() || starting) throw new ServerOnlineException();
        if (new File(folder, data.jarName + ".tmp").exists()) throw new Exception("Minecraft server jar still downloading...");
        if (!new File(folder, data.jarName).exists()) throw new FileNotFoundException(data.jarName + " not found.");
        User user = Settings.getUserByName(getOwner());
        if (user == null) throw new Exception("No owner set??");
        if (user.getMaxRamLeft() != -1 && getRamMax() > user.getMaxRamLeft()) throw new Exception("Out of usable RAM. Lower your max RAM.");
        saveProperties();
        starting = true;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                logger.info("Starting server ................");
                try
                {
                    /**
                     * Build arguments list.
                     */
                    List<String> arguments = new ArrayList<>();
                    arguments.add(Constants.JAVAPATH);
                    arguments.add("-server");
                    {
                        int amount = getRamMin();
                        if (amount > 0) arguments.add(String.format("-Xms%dM", amount));
                        amount = getRamMax();
                        if (amount > 0) arguments.add(String.format("-Xmx%dM", amount));
                        amount = getPermGen();
                        if (amount > 0) arguments.add(String.format("-XX:MaxPermSize=%dm", amount));
                    }
                    for (String s : data.extraJavaParameters) if (s.trim().length() != 0) arguments.add(s.trim());
                    arguments.add("-jar");
                    arguments.add(data.jarName);
                    arguments.add("nogui");
                    for (String s : data.extraMCParameters) if (s.trim().length() != 0) arguments.add(s.trim());

                    // Debug printout
                    logger.info("Arguments: " + arguments.toString());

                    /**
                     * Make ProcessBuilder, set rundir, and make sure the io gets redirected
                     */
                    ProcessBuilder pb = new ProcessBuilder(arguments);
                    pb.directory(folder);
                    pb.redirectErrorStream(true);
                    process = pb.start();
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                last25LogLines.clear();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null)
                                {
                                    logger.info(line);
                                    last25LogLines.addLast(line);
                                    if (last25LogLines.size() > 25) last25LogLines.removeFirst();
                                }
                            }
                            catch (IOException e)
                            {
                                logger.error(e);
                            }
                        }
                    }, data.name.concat("-streamEater")).start();

                    /**
                     * Renews cashed vars so they are up to date when the page is refreshed.
                     */
                    lastRcon = 0L;
                    getRCon();
                    lastQuery = 0L;
                    checkCachedResponse();
                }
                catch (IOException e)
                {
                    logger.error(e);
                }
                starting = false;
            }
        }, "ServerStarter-" + getName()).start(); // <-- Very important call.
    }

    /**
     * Stop the server gracefully
     *
     * @return true if successful via RCon
     * @throws ServerOnlineException
     */
    @SuppressWarnings("UnusedDeclaration")
    public boolean stopServer() throws ServerOnlineException
    {
        if (!getOnline()) return false;
        logger.info("Stopping server ................");
        try
        {
            getRCon().stop();
            return true;
        }
        catch (Exception e)
        {
            PrintWriter printWriter = new PrintWriter(process.getOutputStream());
            printWriter.println("stop");
            printWriter.flush();
            return false;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean forceStopServer() throws Exception
    {
        if (!getOnline()) throw new ServerOfflineException();
        logger.warn("Killing server process!");
        process.destroy();
        try
        {
            process.getOutputStream().close();
        }
        catch (IOException ignored) {}
        try
        {
            process.getErrorStream().close();
        }
        catch (IOException ignored) {}
        try
        {
            process.getInputStream().close();
        }
        catch (IOException ignored) {}
        process.destroy();
        process.exitValue();
        return true;
    }

    public Process getProcess()
    {
        return process;
    }

    public boolean canUserControl(User user)
    {
        if (user == null) return false;
        if (user.getGroup() == Group.ADMIN || user.getUsername().equals(getOwner())) return true;
        for (String admin : getAdmins()) if (admin.equals(user.getUsername())) return true;
        return false;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void delete() throws ServerOnlineException, IOException
    {
        if (getOnline()) throw new ServerOnlineException();
        FileUtils.deleteDirectory(folder);
        Settings.SETTINGS.servers.remove(getName());
    }

    /**
     * Remove the old and download the new server jar file
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setVersion(String version) throws ServerOnlineException, IOException
    {
        if (getOnline()) throw new ServerOnlineException();
        File jarfile = new File(folder, getJarName());
        if (jarfile.exists()) jarfile.delete();
        File tempFile = new File(folder, getJarName() + ".tmp");
        FileUtils.copyURLToFile(new URL(Constants.JAR_URL.replace("%ID%", version)), tempFile);
        tempFile.renameTo(jarfile);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void downloadModpack(String zipURL, boolean purge) throws IOException, ZipException
    {
        if (purge) FileUtils.deleteDirectory(folder);
        folder.mkdir();

        File zip = new File(folder, "modpack.zip");
        FileUtils.copyURLToFile(new URL(URLDecoder.decode(zipURL, "UTF-8")), zip);

        ZipFile zipFile = new ZipFile(zip);
        zipFile.extractAll(folder.getCanonicalPath());
    }

    public File getFolder()
    {
        return folder;
    }

    public String getLast25LogLinesAsText()
    {
        StringBuilder stringBuilder = new StringBuilder();
        synchronized (last25LogLines)
        {
            for (String line : last25LogLines) stringBuilder.append(line).append('\n');
        }
        return stringBuilder.toString();
    }

    public void send(String s)
    {
        PrintWriter printWriter = new PrintWriter(process.getOutputStream());
        printWriter.println(s);
        printWriter.flush();
    }

    /*
     ---------------------------------------------------------------- GSON ----------------------------------------------------------------
      */
    public static class Deserializer implements JsonDeserializer<Server>
    {
        @Override
        public Server deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            ServerData serverData = context.deserialize(json, ServerData.class);
            return new Server(serverData);
        }
    }

    public static class Serializer implements JsonSerializer<Server>
    {
        @Override
        public JsonElement serialize(Server src, Type typeOfSrc, JsonSerializationContext context)
        {
            return context.serialize(src.data);
        }
    }
}
