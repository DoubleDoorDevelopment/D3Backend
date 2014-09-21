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
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import org.apache.commons.io.FileUtils;

import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;

import static net.doubledoordev.backend.Main.LOGGER;
import static net.doubledoordev.backend.util.Constants.*;

/**
 * Global settings
 *
 * @author Dries007
 */
@SuppressWarnings("ALL")
public class Settings
{
    public static final Settings SETTINGS = new Settings();

    static
    {
        try
        {
            FileReader fileReader;
            if (CONFIG_FILE.exists())
            {
                fileReader = new FileReader(CONFIG_FILE);
                JsonObject jsonElement = Constants.JSONPARSER.parse(fileReader).getAsJsonObject();
                if (jsonElement.has("hostname")) SETTINGS.hostname =  jsonElement.get("hostname").getAsString();
                if (jsonElement.has("port")) SETTINGS.port = jsonElement.get("port").getAsInt();
                if (jsonElement.has("useJava8")) SETTINGS.useJava8 = jsonElement.get("useJava8").getAsBoolean();
                if (jsonElement.has("fixedPorts")) SETTINGS.fixedPorts = jsonElement.get("fixedPorts").getAsBoolean();
                if (jsonElement.has("fixedIP")) SETTINGS.fixedIP = jsonElement.get("fixedIP").getAsBoolean();
                if (jsonElement.has("portRange")) SETTINGS.portRange = GSON.fromJson(jsonElement.getAsJsonObject("portRange"), PortRange.class);

                if (jsonElement.has("anonPages"))
                {
                    SETTINGS.anonPages = new ArrayList<>();
                    JsonArray array = jsonElement.getAsJsonArray("anonPages");
                    for (int i = 0; i < array.size(); i ++)
                        SETTINGS.anonPages.add(array.get(i).getAsString());
                }
                fileReader.close();
            }

            if (SERVERS_FILE.exists())
            {
                fileReader = new FileReader(SERVERS_FILE);
                if (SERVERS_FILE.exists())
                    for (Server server : GSON.fromJson(fileReader, Server[].class))
                        SETTINGS.servers.put(server.getName(), server);
                fileReader.close();
            }

            if (USERS_FILE.exists())
            {
                fileReader = new FileReader(USERS_FILE);
                if (USERS_FILE.exists())
                    for (User user : GSON.fromJson(fileReader, User[].class))
                        SETTINGS.users.put(user.getUsername(), user);
                fileReader.close();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Server> servers = new HashMap<>();
    public Map<String, User> users = new HashMap<>();

    public String hostname;
    public int port = 80;
    public boolean useJava8 = false;
    public boolean fixedPorts = false;
    public boolean fixedIP = false;
    public PortRange portRange = new PortRange();
    public List<String> anonPages = Arrays.asList("index", "login", "register");

    private Settings()
    {
        try
        {
            hostname = Inet4Address.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            hostname = "";
        }
    }

    public static void save()
    {
        try
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("hostname", SETTINGS.hostname);
            jsonObject.addProperty("port", SETTINGS.port);
            jsonObject.addProperty("useJava8", SETTINGS.useJava8);
            jsonObject.addProperty("fixedPorts", SETTINGS.fixedPorts);
            jsonObject.addProperty("fixedIP", SETTINGS.fixedIP);
            JsonArray anonPages = new JsonArray();
            for (String s : SETTINGS.anonPages) anonPages.add(new JsonPrimitive(s));
            jsonObject.add("anonPages", anonPages);
            jsonObject.add("portRange", GSON.toJsonTree(SETTINGS.portRange));
            FileUtils.writeStringToFile(CONFIG_FILE, GSON.toJson(jsonObject));

            FileUtils.writeStringToFile(SERVERS_FILE, GSON.toJson(SETTINGS.servers.values()));
            FileUtils.writeStringToFile(USERS_FILE, GSON.toJson(SETTINGS.users.values()));

            LOGGER.info("Saved settings.");
        }
        catch (IOException e)
        {
            LOGGER.error("Error saving the config file...", e);
        }
    }

    public Collection<Server> getServers()
    {
        return servers.values();
    }

    public Collection<User> getUsers()
    {
        return users.values();
    }

    public static Server getServerByName(String name)
    {
        return SETTINGS.servers.get(name);
    }

    public static User getUserByName(String name)
    {
        return SETTINGS.users.get(name);
    }

    public String getHostname()
    {
        return hostname;
    }

    public int getPort()
    {
        return port;
    }

    public boolean isUseJava8()
    {
        return useJava8;
    }

    public boolean isFixedPorts()
    {
        return fixedPorts;
    }

    public boolean isFixedIP()
    {
        return fixedIP;
    }

    public PortRange getPortRange()
    {
        return portRange;
    }
}
