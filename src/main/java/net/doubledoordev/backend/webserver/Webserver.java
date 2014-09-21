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

package net.doubledoordev.backend.webserver;

import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import static net.doubledoordev.backend.Main.LOGGER;
import static net.doubledoordev.backend.util.Constants.COOKIE_KEY;

/**
 * Format for GET requests:
 *  ->  /static/... => served as a regular file.
 *  ->  /...        => Served from a template.
 *
 * Format for PUT requests:
 *  ->  /server/${serverName}/${methodName}/${parameters ...}   =>  Invoke method on a specific server.
 *
 * @author Dries007
 */
public class Webserver extends SimpleWebServer
{
    public static final Webserver WEBSERVER = new Webserver();
    private static final String STATIC_PATH = "/static/";

    private Webserver()
    {
        super(Settings.SETTINGS.hostname, Settings.SETTINGS.port, "/static/");
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        DataObject dataObject = new DataObject(session);
        Main.printdebug(session, dataObject);
        switch (session.getMethod())
        {
            case POST:
                handlePost(dataObject, session);
            case GET:
                return serveGet(dataObject, session);
            case PUT:
                return servePut(dataObject, session);
        }
        return new Response(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "No Content");
    }

    private void handlePost(DataObject dataObject, IHTTPSession session)
    {
        try
        {
            session.parseBody(new HashMap<String, String>());
            Map<String, String> map = session.getParms();
            String split[] = session.getUri().substring(1).split("/");
            switch (split[0]) // 0 => type id
            {
                case "login":
                    if (map.containsKey("username") && map.containsKey("password"))
                    {
                        User user = DataObject.getUserByName(map.get("username"));
                        if (user != null && user.verify(map.get("password")))
                        {
                            session.getCookies().set(COOKIE_KEY, user.getUsername() + "|" + user.getPasshash(), 30);
                            dataObject.setUser(user);
                        }
                        else dataObject.adapt(new Exception("Login failed."));
                    }
                    else if (map.containsKey("logout"))
                    {
                        session.getCookies().delete(COOKIE_KEY);
                        dataObject.setUser(null);
                    }
                    else if (dataObject.getUser() != null && map.containsKey("oldPassword") && map.containsKey("newPassword"))
                    {
                        if (dataObject.getUser().updatePassword(map.get("oldPassword"), map.get("newPassword")))
                        {
                            session.getCookies().set(COOKIE_KEY, dataObject.getUser().getUsername() + "|" + dataObject.getUser().getPasshash(), 30);
                        }
                        else dataObject.adapt(new Exception("Old password was wrong."));
                    }
                    else dataObject.adapt(new Exception("Form error."));
                    break;
                case "register":
                    if (map.containsKey("username") && map.containsKey("password") && map.containsKey("areyouhuman"))
                    {
                        if (!map.get("areyouhuman").trim().equals("4")) dataObject.adapt(new Exception("You failed the human test..."));
                        else
                        {
                            User user = DataObject.getUserByName(map.get("username"));
                            if (!Constants.USERNAME_CHECK.matcher(map.get("username")).matches())
                            {
                                dataObject.adapt(new Exception("Username contains invalid chars.<br>Only a-Z, 0-9, _ and - please."));
                            }
                            else if (user == null)
                            {
                                try
                                {
                                    user = new User(map.get("username"), PasswordHash.createHash(map.get("password")));
                                    Settings.SETTINGS.users.add(user);
                                    session.getCookies().set(COOKIE_KEY, user.getUsername() + "|" + user.getPasshash(), 30);
                                    dataObject.setUser(user);
                                    Settings.save();
                                }
                                catch (NoSuchAlgorithmException | InvalidKeySpecException e)
                                {
                                    throw new RuntimeException(e);
                                }
                            }
                            else dataObject.adapt(new Exception("Username taken."));
                        }
                    }
                    else dataObject.adapt(new Exception("Form error."));
                    break;
            }
        }
        catch (IOException | ResponseException e)
        {
            e.printStackTrace();
        }
    }

    private Response servePut(DataObject dataObject, IHTTPSession session)
    {
        try
        {
            //                                  \/-> eliminate first "/"
            String split[] = session.getUri().substring(1).split("/");
            switch (split[0]) // 0 => type id
            {
                case "server":
                    return invokeWithRefectionMagic(DataObject.getServerByName(split[1]), split);
                case "users":
                    return invokeWithRefectionMagic(DataObject.getUserByName(split[1]), split);
                // ----------------------------------------------------------------------------------------------------------
                case "console":
                    Server server = DataObject.getServerByName(split[1]);
                    if (!server.getOnline()) return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Server Offline.");
                    return new Response(Response.Status.OK, MIME_PLAINTEXT, server.getRCon().send(split[2]));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(); // TODO: remove
            return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getLocalizedMessage());
        }
        return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Method not found");
    }

    private Response invokeWithRefectionMagic(Object instance, String[] split) throws Exception
    {
        for (java.lang.reflect.Method method : instance.getClass().getDeclaredMethods())
        {
            // Check to see if name is same and if the amount of parameters fits.
            // 2 => method name,    -3 => compensation for type, server name and method name.
            if (method.getName().equals(split[2]) && method.getParameterTypes().length == split.length - 3)
            {
                try
                {
                    Object parms[] = new Object[split.length - 3];
                    for (int i = 0; i < method.getParameterTypes().length; i++)
                        parms[i] = TypeHellhole.convert(method.getParameterTypes()[i], split[i + 3]);
                    method.invoke(instance, parms);
                    return new Response(Response.Status.OK, MIME_PLAINTEXT, "OK");
                }
                catch (Exception ignored)
                {
                    // Ignored because we don't care.
                }
            }
        }
        return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Server method not found");
    }

    private Response serveGet(DataObject dataObject, IHTTPSession session)
    {
        String uri = session.getUri().toLowerCase();
        if (uri.startsWith(STATIC_PATH)) return super.respond(session.getHeaders(), uri.substring(STATIC_PATH.length()));

        try
        {
            if (uri.equals("/")) uri += "index";
            uri = uri.substring(1);
            if (uri.endsWith("/")) uri = uri.substring(0, uri.length() - 1);
            return new Response(PageResolver.resolve(dataObject, uri, session));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getLocalizedMessage());
        }
    }
}
