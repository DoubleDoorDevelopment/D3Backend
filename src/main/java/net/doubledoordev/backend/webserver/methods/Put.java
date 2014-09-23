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
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.TypeHellhole;
import net.doubledoordev.backend.webserver.NanoHTTPD;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static net.doubledoordev.backend.webserver.NanoHTTPD.MIME_PLAINTEXT;
import static net.doubledoordev.backend.webserver.NanoHTTPD.Response;
import static net.doubledoordev.backend.webserver.NanoHTTPD.Response.Status.*;

/**
 * Processes PUT requests
 * <p/>
 * Basically allows a put request to call a method or send commands to a server
 *
 * @author Dries007
 */
public class Put
{
    private Put()
    {
    }

    /**
     * Entry point
     */
    public static Response handlePut(HashMap<String, Object> dataObject, NanoHTTPD.IHTTPSession session)
    {
        try
        {
            session.parseBody(new HashMap<String, String>());
            Main.printdebug(session, dataObject);
            Map<String, String> map = session.getParms();
            String[] split = new String[Integer.parseInt(map.get("lengh"))];
            for (int i = 0; i < split.length; i++)
                split[i] = map.get("p" + i);
            switch (map.get("p0")) // 0 => type id
            {
                case "server":
                    Server server = Settings.getServerByName(split[1]);
                    if (!server.canUserControl((User) dataObject.get("user"))) return new Response(FORBIDDEN, MIME_PLAINTEXT, "Forbidden");
                    return invokeWithRefectionMagic(server, split);
                // ----------------------------------------------------------------------------------------------------------
                case "users":
                    if (!(boolean) dataObject.get("admin")) return new Response(FORBIDDEN, MIME_PLAINTEXT, "Forbidden");
                    return invokeWithRefectionMagic(Settings.getUserByName(split[1]), split);
                // ----------------------------------------------------------------------------------------------------------
                case "console":
                    server = Settings.getServerByName(split[1]);
                    if (!server.canUserControl((User) dataObject.get("user"))) return new Response(FORBIDDEN, MIME_PLAINTEXT, "Forbidden");
                    if (!server.getOnline())
                        return new Response(INTERNAL_ERROR, MIME_PLAINTEXT, "Server Offline.");
                    server.send(split[2]);
                    return new Response(OK, MIME_PLAINTEXT, "");
            }
        }
        catch (Exception e)
        {
            return new Response(INTERNAL_ERROR, MIME_PLAINTEXT, e.toString());
        }
        return new Response(NOT_FOUND, MIME_PLAINTEXT, "Method not found");
    }

    private static Response invokeWithRefectionMagic(Object instance, String[] split) throws Exception
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
                    return new Response(OK, MIME_PLAINTEXT, "OK");
                }
                catch (ClassCastException ignored)
                {
                    // Ignored because we don't care.
                }
                catch (InvocationTargetException e)
                {
                    Main.LOGGER.warn(e.getCause());
                    return new Response(INTERNAL_ERROR, MIME_PLAINTEXT, e.getCause().toString());
                }
            }
        }
        return new Response(NOT_FOUND, MIME_PLAINTEXT, "Server method not found");
    }
}
