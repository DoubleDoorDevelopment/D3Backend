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
 *
 */

package net.doubledoordev.backend.webserver.methods;

import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.Group;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.server.ServerData;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.PasswordHash;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.webserver.NanoHTTPD;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static net.doubledoordev.backend.util.Constants.COOKIE_KEY;
import static net.doubledoordev.backend.webserver.NanoHTTPD.IHTTPSession;
import static net.doubledoordev.backend.webserver.NanoHTTPD.ResponseException;

/**
 * Processes POST requests
 *
 * @author Dries007
 */
public class Post
{
    private Post()
    {
    }

    /**
     * Entry point
     */
    public static void handlePost(HashMap<String, Object> dataObject, IHTTPSession session)
    {
        try
        {
            session.parseBody(new HashMap<String, String>());
            Map<String, String> map = session.getParms();
            String split[] = session.getUri().substring(1).split("/");
            switch (split[0]) // 0 => type id
            {
                case "login":
                    handleLogin(dataObject, session, map);
                    break;
                case "register":
                    handleRegister(dataObject, session, map);
                    break;
                case "newserver":
                    try
                    {
                        handleNewServer(dataObject, session, map);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        dataObject.put("message", e.getLocalizedMessage());
                    }
                    break;
            }
        }
        catch (IOException | ResponseException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Handle post requests from the newServer page
     */
    private static void handleNewServer(HashMap<String, Object> dataObject, IHTTPSession session, Map<String, String> map) throws Exception
    {
        User user = (User) dataObject.get("user");
        if (user == null) throw new Exception("Not logged in.");
        if (user.getMaxServers() != -1 && user.getServerCount() + 1 >= user.getMaxServers()) throw new Exception("Max server count reached.");
        ServerData data = new ServerData();

        data.name = user.getUsername() + "_" + map.get("name");
        if (Settings.getServerByName(data.name) != null) throw new Exception("Duplicate server name");

        data.ramMin = Integer.parseInt(map.get("RAMmin"));
        data.ramMax = Integer.parseInt(map.get("RAMmax"));
        if (data.ramMax < data.ramMin)
        {
            int temp = data.ramMax;
            data.ramMax = data.ramMin;
            data.ramMin = temp;
        }
        if (user.getMaxRam() != -1 && user.getMaxRamLeft() < data.ramMax) throw new Exception("You are over your max RAM.");
        if (data.ramMax < 2 || data.ramMin < 2) throw new Exception("RAM settings invalid.");

        data.permGen = Integer.parseInt(map.get("PermGen"));
        if (data.permGen < 2) throw new Exception("PermGen settings invalid.");

        if (user.getGroup() == Group.ADMIN && map.containsKey("owner")) data.owner = map.get("owner");
        else data.owner = user.getUsername();

        if (map.get("extraJavaParameters").trim().length() == 0) data.extraJavaParameters = Arrays.asList(map.get("extraJavaParameters").trim().split("\n"));
        if (map.get("extraMCParameters").trim().length() == 0) data.extraMCParameters = Arrays.asList(map.get("extraMCParameters").trim().split("\n"));
        if (map.get("admins").trim().length() == 0) data.admins = Arrays.asList(map.get("admins").trim().split("\n"));

        data.jarName = map.get("jarname");
        data.rconPswd = map.get("rconpass");
        data.serverPort = Settings.SETTINGS.fixedPorts ? Settings.SETTINGS.portRange.getNextAvailablePort() : Integer.parseInt(map.get("serverport"));
        data.rconPort = Settings.SETTINGS.fixedPorts ? Settings.SETTINGS.portRange.getNextAvailablePort(data.serverPort) : Integer.parseInt(map.get("rconport"));
        data.ip = map.get("ip");
        data.autoStart = map.containsKey("autostart") && map.get("autostart").equals("on");

        Server server = new Server(data);
        Settings.SETTINGS.servers.put(data.name, server);
        Settings.save();
        dataObject.put("step2", true);
        dataObject.put("server", server);

        FileUtils.writeStringToFile(new File(server.getFolder(), "eula.txt"),
                "#The server owner indicated to agree with the EULA when submitting the from that produced this server instance.\n" +
                "#That means that there is no need for extra halting of the server startup sequence with this stupid file.\n" +
                "#" + new Date().toString() + "\n" +
                "eula=true\n");
    }

    /**
     * Handle post requests from the register page
     */
    private static void handleRegister(HashMap<String, Object> dataObject, IHTTPSession session, Map<String, String> map)
    {
        if (map.containsKey("username") && map.containsKey("password") && map.containsKey("areyouhuman"))
        {
            if (!map.get("areyouhuman").trim().equals("4"))
                dataObject.put("message", "You failed the human test...");
            else
            {
                User user = Settings.getUserByName(map.get("username"));
                if (!Constants.USERNAME_CHECK.matcher(map.get("username")).matches())
                {
                    dataObject.put("message", "Username contains invalid chars.<br>Only a-Z, 0-9, _ and - please.");
                }
                else if (user == null)
                {
                    try
                    {
                        user = new User(map.get("username"), PasswordHash.createHash(map.get("password")));
                        Settings.SETTINGS.users.put(user.getUsername(), user);
                        session.getCookies().set(COOKIE_KEY, user.getUsername() + "|" + user.getPasshash(), 30);
                        dataObject.put("user", user);
                        Settings.save();
                    }
                    catch (NoSuchAlgorithmException | InvalidKeySpecException e)
                    {
                        // Hash algorithm doesn't work.
                        throw new RuntimeException(e);
                    }
                }
                else dataObject.put("message", "Username taken.");
            }
        }
        else dataObject.put("message", "Form error.");
    }

    /**
     * Handle post requests from the login page
     */
    private static void handleLogin(HashMap<String, Object> dataObject, IHTTPSession session, Map<String, String> map)
    {
        if (map.containsKey("username") && map.containsKey("password"))
        {
            User user = Settings.getUserByName(map.get("username"));
            if (user != null && user.verify(map.get("password")))
            {
                session.getCookies().set(COOKIE_KEY, user.getUsername() + "|" + user.getPasshash(), 30);
                dataObject.put("user", user);
            }
            else dataObject.put("message", "Login failed.");
        }
        else if (map.containsKey("logout"))
        {
            session.getCookies().delete(COOKIE_KEY);
            dataObject.remove("user");
        }
        else if (dataObject.containsKey("user") && map.containsKey("oldPassword") && map.containsKey("newPassword"))
        {
            User user = (User) dataObject.get("user");
            if (user.updatePassword(map.get("oldPassword"), map.get("newPassword")))
            {
                session.getCookies().set(COOKIE_KEY, user.getUsername() + "|" + user.getPasshash(), 30);
            }
            else dataObject.put("message", "Old password was wrong.");
        }
        else dataObject.put("message", "Form error.");
    }
}
