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

import com.google.gson.annotations.Expose;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import org.apache.commons.io.FileUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static net.doubledoordev.backend.Main.LOGGER;
import static net.doubledoordev.backend.util.Constants.*;

/**
 * Global settings
 *
 * @author Dries007
 */
@SuppressWarnings("UnusedDeclaration")
public class Settings
{
    public static final Settings SETTINGS;

    static
    {
        try
        {
            if (CONFIG_FILE.exists()) SETTINGS = Constants.GSON.fromJson(new FileReader(CONFIG_FILE), Settings.class);
            else
            {
                SETTINGS = new Settings();
                save();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Server> servers = new HashMap<>();
    public Map<String, User> users = new HashMap<>();
    @Expose
    public String hostname = "";
    @Expose
    public int portHTTP = 80;
    @Expose
    public int portHTTPS = 443;
    @Expose
    public boolean fixedPorts = false;
    @Expose
    public boolean fixedIP = false;
    @Expose
    public PortRange portRange = new PortRange();
    @Expose
    public int defaultDiskspace = -1;
    @Expose
    public List<String> anonPages = Arrays.asList("index", "login", "register");
    @Expose
    public String certificatePath = "";
    @Expose
    public String certificatePass = "";

    private Settings() throws IOException
    {
        try
        {
            FileReader fileReader;

            if (SERVERS_FILE.exists())
            {
                fileReader = new FileReader(SERVERS_FILE);
                if (SERVERS_FILE.exists())
                {
                    for (Server server : GSON.fromJson(fileReader, Server[].class))
                    {
                        servers.put(server.getID(), server);
                    }
                }
                fileReader.close();
            }

            if (USERS_FILE.exists())
            {
                fileReader = new FileReader(USERS_FILE);
                if (USERS_FILE.exists())
                {
                    for (User user : GSON.fromJson(fileReader, User[].class))
                    {
                        users.put(user.getUsername().toLowerCase(), user);
                    }
                }
                fileReader.close();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void save()
    {
        try
        {
            FileUtils.writeStringToFile(CONFIG_FILE, GSON.toJson(SETTINGS));
            FileUtils.writeStringToFile(SERVERS_FILE, GSON.toJson(SETTINGS.servers.values()));
            FileUtils.writeStringToFile(USERS_FILE, GSON.toJson(SETTINGS.users.values()));

            LOGGER.info("Saved settings.");
        }
        catch (Exception e)
        {
            LOGGER.error("Error saving the config file...", e);
        }
    }

    public static Server getServerByName(String name)
    {
        return SETTINGS.servers.get(name);
    }

    public static User getUserByName(String name)
    {
        return SETTINGS.users.get(name.toLowerCase());
    }

    public Collection<Server> getServers()
    {
        return servers.values();
    }

    public Collection<Server> getOnlineServers()
    {
        HashSet<Server> onlineServers = new HashSet<>();
        for (Server server : getServers()) if (server.getOnline()) onlineServers.add(server);
        return onlineServers;
    }

    public Collection<User> getUsers()
    {
        return users.values();
    }

    public String getHostname()
    {
        return hostname;
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
