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
 *
 */

package net.doubledoordev.backend.web.socket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.FileManager;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Helper;
import net.doubledoordev.backend.util.WebSocketHelper;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.ArrayList;
import java.util.TimerTask;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class FileManagerSocketApplication extends ServerWebSocketApplication
{
    private static final FileManagerSocketApplication APPLICATION = new FileManagerSocketApplication();
    private static final String                       URL_PATTERN = "/filemanager/*";

    private FileManagerSocketApplication()
    {
        TIMER.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                for (WebSocket socket : getWebSockets()) socket.sendPing("ping".getBytes());
            }
        }, SOCKET_PING_TIME, SOCKET_PING_TIME);
    }

    public static void sendFile(String path, String text)
    {
        for (WebSocket socket1 : APPLICATION.getWebSockets())
        {
            FileManager fileManager = (FileManager) ((DefaultWebSocket) socket1).getUpgradeRequest().getAttribute(FILE_MANAGER);
            if (fileManager.getFile().getAbsolutePath().equals(path))
            {
                WebSocketHelper.sendData(socket1, text);
            }
        }
    }

    public static void register()
    {
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, URL_PATTERN, APPLICATION);
    }

    @Override
    public void onConnect(WebSocket socket)
    {
        super.onConnect(socket);
        String[] split = ((DefaultWebSocket) socket).getUpgradeRequest().getPathInfo().substring(1).split("/", 2);
        FileManager fileManager = new FileManager((Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER), split[1]);
        ((DefaultWebSocket) socket).getUpgradeRequest().setAttribute(FILE_MANAGER, fileManager);
    }

    @Override
    public void onMessage(WebSocket socket, String text)
    {
        FileManager fileManager = (FileManager) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(FILE_MANAGER);
        if (!fileManager.getServer().isCoOwner((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER)))
        {
            WebSocketHelper.sendError(socket, "You have no rights to this server.");
            socket.close();
            return;
        }
        try
        {
            JsonObject object = JSONPARSER.parse(text).getAsJsonObject();
            String name = object.get("method").getAsString();
            ArrayList<String> args = new ArrayList<>();
            if (object.has("args")) for (JsonElement arg : object.getAsJsonArray("args")) args.add(arg.getAsString());
            if (!Helper.invokeWithRefectionMagic(socket, fileManager, name, args))
            {
                WebSocketHelper.sendOk(socket);
                socket.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            WebSocketHelper.sendError(socket, e);
        }
    }
}
