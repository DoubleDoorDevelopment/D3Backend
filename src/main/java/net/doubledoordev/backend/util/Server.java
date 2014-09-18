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

import com.google.gson.*;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.util.exceptions.ServerOnlineException;
import net.doubledoordev.backend.util.query.MCQuery;
import net.doubledoordev.backend.util.query.QueryResponse;
import net.doubledoordev.backend.util.rcon.RCon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.util.*;

import static net.doubledoordev.backend.util.Constants.LOCALHOST;
import static net.doubledoordev.backend.util.Constants.NAME;
import static net.doubledoordev.backend.util.Constants.SERVERS;

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
    private static final String RCON_PORT = "rcon.port";
    private static final String SERVER_IP = "server-ip";
    private static final int CACHE_TIMEOUT = 1000 * 10;

    private final ServerData data;

    private Logger logger;
    private File folder;
    private File propertiesFile;
    private boolean propertiesLoaded = false;
    private Properties properties = new Properties();

    private RCon rCon;
    private long lastRcon = 0L;

    private MCQuery query;
    private long lastQuery = 0L;
    private QueryResponse cachedResponse;

    private Process process;

    public Server(ServerData data)
    {
        this.data = data;
        this.logger = LogManager.getLogger(data.name);
        this.folder = new File(SERVERS, data.folderName);
        this.propertiesFile = new File(folder, SERVER_PROPERTIES);

        if (folder.exists()) saveProperties();
        else Main.LOGGER.warn("Server folder " + folder.getName() + " does not exist.");

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
                        for (String user : getPlayerList()) rCon.send("kick", user, NAME + " is taking over! Server Reboot!");
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

        if (!getOnline() && getAutoStart())
        {
            try
            {
                startServer();
            }
            catch (ServerOnlineException e)
            {
                //
            }
        }
    }

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

    public MCQuery getQuery()
    {
        if (query == null) query = new MCQuery(LOCALHOST, data.serverPort);
        return query;
    }

    public Properties getProperties()
    {
        propertiesLoaded = true;
        if (propertiesFile.exists())
        {
            try
            {
                properties.load(new FileReader(propertiesFile));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return properties;
    }

    public void saveProperties()
    {
        if (!propertiesLoaded) getProperties();

        if (!data.fixedServerPort)
        {
            properties.setProperty(SERVER_PORT, String.valueOf(data.serverPort));
            properties.setProperty(QUERY_PORT, String.valueOf(data.serverPort));
        }
        else
        {
            data.serverPort = Integer.parseInt(properties.getProperty(SERVER_PORT, String.valueOf(data.serverPort)));
            properties.setProperty(QUERY_PORT, String.valueOf(data.serverPort));
        }

        if (!data.fixedIP) properties.setProperty(SERVER_IP, data.ip);
        else data.ip = properties.getProperty(SERVER_IP, data.ip);

        if (!data.fixedRConPort) properties.setProperty(RCON_PORT, String.valueOf(data.rconPort));
        else data.rconPort = Integer.parseInt(properties.getProperty(RCON_PORT, String.valueOf(data.rconPort)));

        try
        {
            if (!propertiesFile.exists()) //noinspection ResultOfMethodCallIgnored
                propertiesFile.createNewFile();
            properties.store(new FileWriter(propertiesFile), "Minecraft server properties\nModified by D3Backend");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void checkCachedResponse()
    {
        if (System.currentTimeMillis() - lastQuery > CACHE_TIMEOUT)
        {
            cachedResponse = getQuery().fullStat();
            lastQuery = System.currentTimeMillis();
        }
    }

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

    public boolean isPortAvailable(int port)
    {
        try
        {
            ServerSocket socket = new ServerSocket();
            socket.bind(data.ip == null || data.ip.trim().length() == 0 ? new InetSocketAddress(port) : new InetSocketAddress(data.ip, port));
            socket.close();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public Object get(String name) throws InvocationTargetException, IllegalAccessException
    {
        for (Method method : this.getClass().getDeclaredMethods())
            if (method.getName().equalsIgnoreCase("get" + name)) return method.invoke(this);
        return null;
    }

    public String getDisplayAddress()
    {
        StringBuilder builder = new StringBuilder(25);
        if (data.ip == null || data.ip.trim().length() == 0) builder.append(LOCALHOST);
        else builder.append(data.ip);
        builder.append(':').append(data.serverPort);
        return builder.toString();
    }

    public void setProperty(String key, String value) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        properties.put(key, value);
        saveAll();
    }

    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }

    public Enumeration<Object> getPropertyKeys()
    {
        return properties.keys();
    }

    public int getServerPort()
    {
        return Integer.parseInt(getProperty(SERVER_PORT));
    }

    public int getRconPort()
    {
        return Integer.parseInt(getProperty(RCON_PORT));
    }

    public int getOnlinePlayers()
    {
        checkCachedResponse();
        return cachedResponse == null ? -1 : cachedResponse.getOnlinePlayers();
    }

    public int getSlots()
    {
        checkCachedResponse();
        return cachedResponse == null ? -1 : cachedResponse.getMaxPlayers();
    }

    public String getMotd()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getMotd();
    }

    public String getGameMode()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getGameMode();
    }

    public String getMapName()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getMapName();
    }

    public ArrayList<String> getPlayerList()
    {
        checkCachedResponse();
        return cachedResponse == null ? new ArrayList<String>() : cachedResponse.getPlayerList();
    }

    public String getPlugins()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getPlugins();
    }

    public String getVersion()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getVersion();
    }

    public String getGameID()
    {
        checkCachedResponse();
        return cachedResponse == null ? "?" : cachedResponse.getGameID();
    }

    public String getName()
    {
        return data.name;
    }

    public boolean getFixedServerPort()
    {
        return data.fixedServerPort;
    }

    public boolean getFixedRConPort()
    {
        return data.fixedRConPort;
    }

    public boolean getFixedIP()
    {
        return data.fixedIP;
    }

    public int getRamMin()
    {
        return data.ramMin;
    }

    public int getRamMax()
    {
        return data.ramMax;
    }

    public int getPermGen()
    {
        return data.permGen;
    }

    public List<String> getExtraJavaParameters()
    {
        return data.extraJavaParameters;
    }

    public List<String> getExtraMCParameters()
    {
        return data.extraMCParameters;
    }

    public String getFolderName()
    {
        return data.folderName;
    }

    public String getJarName()
    {
        return data.jarName;
    }

    public boolean getAutoStart()
    {
        return data.autoStart;
    }

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
            properties.load(new StringReader(urlEncodedText));
            saveAll();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void setAutoStart(boolean autoStart)
    {
        logger.error("setAutoStart " + autoStart);
        data.autoStart = autoStart;
        saveAll();
    }

    public void setRamMin(int ramMin) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.ramMin = ramMin;
        saveAll();
    }

    public void setRamMax(int ramMax) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.ramMax = ramMax;
        saveAll();
    }

    public void setPermGen(int permGen) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.permGen = permGen;
        saveAll();
    }

    public void setExtraJavaParameters(List<String> list) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.extraJavaParameters = list;
        saveAll();
    }

    public void setExtraMCParameters(List<String> list) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.extraMCParameters = list;
        saveAll();
    }

    public void setExtraJavaParameters(String list) throws ServerOnlineException
    {
        setExtraJavaParameters(Arrays.asList(list.split(",")));
    }

    public void setExtraMCParameters(String list) throws ServerOnlineException
    {
        setExtraMCParameters(Arrays.asList(list.split(",")));
    }

    public void setExtraJavaParameters() throws ServerOnlineException
    {
        setExtraJavaParameters(Arrays.asList(new String[0]));
    }

    public void setExtraMCParameters() throws ServerOnlineException
    {
        setExtraMCParameters(Arrays.asList(new String[0]));
    }

    public void setJarName(String jarName) throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        data.jarName = jarName;
        saveAll();
    }

    public void startServer() throws ServerOnlineException
    {
        if (getOnline()) throw new ServerOnlineException();
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                logger.info("Starting server ................");
                try
                {
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

                    arguments.addAll(data.extraJavaParameters);
                    arguments.add("-jar");
                    arguments.add(data.jarName);
                    arguments.add("nogui");
                    arguments.addAll(data.extraMCParameters);

                    logger.info("Arguments: " + arguments.toString());

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
                                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null) logger.info(line);
                            }
                            catch (IOException e)
                            {
                                logger.error(e);
                            }
                        }
                    }, data.name.concat("-streamEater")).start();

                    lastRcon = 0L;
                    getRCon();
                    lastQuery = 0L;
                    checkCachedResponse();
                }
                catch (IOException e)
                {
                    logger.error(e);
                }
            }
        }).start();
    }

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
            logger.error("Error stopping server.", e);
            return false;
        }
    }

    public boolean forceStopServer() throws ServerOnlineException
    {
        if (!getOnline()) return false;
        process.destroy();
        return true;
    }

    private void saveAll()
    {
        saveProperties();
        Settings.SETTINGS.save();
    }

    public Process getProcess()
    {
        return process;
    }

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
