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
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.DataObject;
import net.doubledoordev.backend.util.PageResolver;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.TypeHellhole;

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
        if (!session.getUri().startsWith("/static/") && !session.getUri().equals("/favicon.ico"))
        {
            Main.LOGGER.debug("getParms: " + session.getParms());
            Main.LOGGER.debug("getHeaders: " + session.getHeaders());
            Main.LOGGER.debug("getUri: " + session.getUri());
            Main.LOGGER.debug("getQueryParameterString: " + session.getQueryParameterString());
            Main.LOGGER.debug("getMethod: " + session.getMethod());
            Main.LOGGER.debug("getCookies: " + session.getCookies());
            Main.LOGGER.debug("-----================================-----");
        }

        switch (session.getMethod())
        {
            case POST:
                handlePost(session);
            case GET:
                return serveGet(session);
            case PUT:
                return servePut(session);
        }
        return new Response(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "No Content");
    }

    private void handlePost(IHTTPSession session)
    {

    }

    private Response servePut(IHTTPSession session)
    {
        try
        {
            //                                  \/-> eliminate first "/"
            String split[] = session.getUri().substring(1).split("/");
            switch (split[0]) // 0 => type id
            {
                case "server":
                    Server server = DataObject.getServerByName(split[1]); // 1 => server name
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
                    server = DataObject.getServerByName(split[1]);
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

    private Response serveGet(IHTTPSession session)
    {
        String uri = session.getUri().toLowerCase();
        if (uri.startsWith(STATIC_PATH)) return super.respond(session.getHeaders(), uri.substring(STATIC_PATH.length()));

        try
        {
            if (uri.equals("/")) uri += "index";
            uri = uri.substring(1);
            if (uri.endsWith("/")) uri = uri.substring(0, uri.length() - 1);
            return new Response(PageResolver.resolve(uri, session));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getLocalizedMessage());
        }
    }
}
