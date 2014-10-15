/*
 * Unless otherwise specified through the '@author' tag or comments at
 * the top of the file or on a specific portion of the code the following license applies:
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
 *  The header specified or the above copyright notice, this list of conditions
 *   and the following disclaimer below must be displayed at the top of the source code
 *   of any web page received while using any part of the service this software provides.
 *
 *   The header to be displayed:
 *       This page was generated by DoubleDoorDevelopment's D3Backend or a derivative thereof.
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

import net.doubledoordev.backend.permissions.Group;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.webserver.methods.Get;
import net.doubledoordev.backend.webserver.methods.Post;
import net.doubledoordev.backend.webserver.methods.Put;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static net.doubledoordev.backend.util.Constants.*;
import static net.doubledoordev.backend.util.Settings.SETTINGS;

/**
 * NanoHTTPD based server
 *
 * @author Dries007
 */
public class Webserver extends SimpleWebServer
{
    public static final Webserver WEBSERVER   = new Webserver();
    public              long      lastRequest = System.currentTimeMillis();

    private Webserver()
    {
        super(SETTINGS.hostname, SETTINGS.port, STATIC_PATH);
    }

    /**
     * The entry point for all HTTP requests
     *
     * @param session The HTTP session
     * @return
     */
    @Override
    public Response serve(IHTTPSession session)
    {
        lastRequest = System.currentTimeMillis();

        // We want to split off static ASAP.
        if (session.getUri().startsWith(STATIC_PATH))
            return super.respond(session.getUri().substring(STATIC_PATH.length()));

        // stupid favicon.ico
        if (session.getUri().contains(FAVOTICON))
            return super.respond(session.getUri());

        if (session.getUri().startsWith(P2S_PATH))
            return servePay2SpawnFile(session.getUri().split("/"));

        /**
         * Populate data map with user (if logged in) and admin status.
         */
        HashMap<String, Object> dataObject = new HashMap<>();
        if (session.getCookies().has(COOKIE_KEY))
        {
            String[] cookie = session.getCookies().read(COOKIE_KEY).split("\\|");
            User user = Settings.getUserByName(cookie[0]);
            if (user != null && user.getPasshash().equals(cookie[1]))
            {
                dataObject.put("user", user);
                dataObject.put("admin", user.getGroup() == Group.ADMIN);
            }
        }
        if (!dataObject.containsKey("admin")) dataObject.put("admin", false);

        /**
         * Handle depending on HTTP method.
         * - PUT is a method call
         * - POST is a form submission, also calls GET
         * - GET is a page request
         * - Anything else is invalid.
         */
        switch (session.getMethod())
        {
            case POST:
                Post.handlePost(dataObject, session);
            case GET:
                return Get.handleGet(dataObject, session);
            case PUT:
                return Put.handlePut(dataObject, session);
            default:
                return new Response(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "No Content");
        }
    }

    private Response servePay2SpawnFile(String[] uri)
    {
        if (uri.length != 4) return createResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Error 400, Arguments accepted are only server and name.");
        Server server = Settings.getServerByName(uri[2]);
        if (server == null) return createResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, String.format("Error 400, Server '%s' doesn't exits.", uri[2]));
        File file = new File(server.getFolder(), String.format("pay2spawn/%s.html", uri[3]));
        if (!file.exists()) return createResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, String.format("Error 400, User '%s' does not have a Pay2Spawn file on the server '%s'.", uri[3], uri[2]));
        Response res;
        try
        {
            InputStream stream = new FileInputStream(file);
            int fileLen = stream.available();
            res = createResponse(Response.Status.OK, "text/html", stream);
            res.addHeader("Content-Length", "" + fileLen);
        }
        catch (IOException ioe)
        {
            res = getForbiddenResponse("Reading file failed.");
        }
        return res == null ? getNotFoundResponse() : res;
    }
}
