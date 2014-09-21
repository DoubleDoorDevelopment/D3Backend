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

import net.doubledoordev.backend.permissions.Group;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.webserver.NanoHTTPD;

import java.util.List;

import static net.doubledoordev.backend.util.Constants.COOKIE_KEY;
import static net.doubledoordev.backend.util.Settings.*;

/**
 * This is the object passed to the Freemaker template processor.
 *
 * @author Dries007
 */
public class DataObject
{
    private static List<Server> servers = SETTINGS.servers;
    private static List<User> users  = SETTINGS.users;
    private Server server;
    private User user;
    private String message;

    public DataObject(NanoHTTPD.IHTTPSession session)
    {
        if (session.getCookies().has(COOKIE_KEY))
        {
            String[] cookie = session.getCookies().read(COOKIE_KEY).split("\\|");
            User user = getUserByName(cookie[0]);
            if (user != null && user.getPasshash().equals(cookie[1])) this.user = user;
        }
    }

    public List<Server> getServers()
    {
        return servers;
    }

    public static Server getServerByName(String name)
    {
        if (name == null) return null;
        for (Server server : SETTINGS.servers) if (server.getName().equalsIgnoreCase(name)) return server;
        return null;
    }

    public List<User> getUsers()
    {
        return users;
    }

    public static User getUserByName(String name)
    {
        if (name == null) return null;
        for (User user : SETTINGS.users) if (user.getUsername().equalsIgnoreCase(name)) return user;
        return null;
    }

    public Server getServer()
    {
        return server;
    }

    public User getUser()
    {
        return user;
    }

    public String getMessage()
    {
        return message;
    }

    public boolean getAdmin()
    {
        return user != null && user.getGroup() == Group.ADMIN;
    }

    public int getNextAvailablePort(int ignored) throws PortRange.OutOfPortsException
    {
        return SETTINGS.portRange.getNextAvailablePort(ignored);
    }

    public int getNextAvailablePort() throws PortRange.OutOfPortsException
    {
        return SETTINGS.portRange.getNextAvailablePort();
    }

    public boolean getFixedports()
    {
        return Settings.SETTINGS.fixedPorts;
    }

    public boolean getFixedip()
    {
        return Settings.SETTINGS.fixedIP;
    }

    public String randomString(int length)
    {
        return Constants.randomString(length);
    }

    public DataObject adapt(String[] args)
    {
        if (args.length > 0)
        {
            switch (args[0])
            {
                case "console":
                case "servers":
                    if (args.length > 1) server = getServerByName(args[1]);
                    break;
            }
        }
        return this;
    }

    public Object adapt(Throwable e)
    {
        this.message = e.getLocalizedMessage();
        return this;
    }

    public void setUser(User user)
    {
        this.user = user;
    }
}
