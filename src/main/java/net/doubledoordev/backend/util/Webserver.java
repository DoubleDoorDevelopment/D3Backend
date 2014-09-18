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

import fi.iki.elonen.SimpleWebServer;
import net.doubledoordev.backend.Main;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Format for GET requests:
 *  ->  /static/... => served as a regular file.
 *  ->  /...        => Served from a template.
 *  ->  /.../       => Served as "/.../index".
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
        super(Settings.SETTINGS.getHostname(), Settings.SETTINGS.getPort(), "/static/");
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        if (!session.getUri().startsWith("/static/") && !session.getUri().equals("/favicon.ico"))
        {
            Main.LOGGER.debug("getQueryParameterString: " + session.getQueryParameterString());
            Main.LOGGER.debug("getUri: " + session.getUri());
            Main.LOGGER.debug("getCookies: " + session.getCookies());
            Main.LOGGER.debug("getHeaders: " + session.getHeaders());
            Main.LOGGER.debug("getMethod: " + session.getMethod());
            Main.LOGGER.debug("getParms: " + session.getParms());
            Main.LOGGER.debug("-----================================-----");
        }

        switch (session.getMethod())
        {
            case GET:
                return serveGet(session);
            case PUT:
                return servePut(session);
        }
        return new Response(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "No Content");
    }

    public Response servePut(IHTTPSession session)
    {
        try
        {
            //                                  \/-> eliminate first "/"
            String split[] = session.getUri().substring(1).split("/");
            switch (split[0]) // 0 => type id
            {
                case "server":
                    Server server = Settings.getServerByName(split[1]); // 1 => server name
                    for (java.lang.reflect.Method method : server.getClass().getDeclaredMethods())
                    {
                        // Check to see if name is same and if the amount of parameters fits.
                        // 2 => method name,    -3 => compensation for type, server name and method name.
                        if (method.getName().equals(split[2]) && method.getParameterTypes().length == split.length - 3)
                        {
                            Object parms[] = new Object[split.length - 3];
                            for (int i = 0; i < method.getParameterTypes().length; i++)
                                parms[i] = TypeHellhole.convert(method.getParameterTypes()[i], split[i + 3]);
                            method.invoke(server, parms);
                            return new Response(Response.Status.OK, MIME_PLAINTEXT, "OK");
                        }
                    }
                    return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Server method not found");
                // ----------------------------------------------------------------------------------------------------------
                case "console":
                    server = Settings.getServerByName(split[1]);
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

    public Response serveGet(IHTTPSession session)
    {
        String uri = session.getUri().toLowerCase();
        if (uri.startsWith(STATIC_PATH)) return super.respond(session.getHeaders(), uri.substring(STATIC_PATH.length()));

        try
        {
            if (uri.endsWith("/")) uri += "index";
            uri = uri.substring(1);
            return new Response(PageResolver.resolve(uri, session));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getLocalizedMessage());
        }
    }
}
