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

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Helper;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.WebSocketHelper;
import org.glassfish.grizzly.http.server.DefaultSessionManager;
import org.glassfish.grizzly.http.server.Session;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.TimerTask;

import static net.doubledoordev.backend.util.Constants.*;
import static net.doubledoordev.backend.util.Settings.SETTINGS;

/**
 * Used by the serverlist to keep up to date.
 *
 * @author Dries007
 */
public class ServerMonitorSocketApplication extends WebSocketApplication
{
    private static final ServerMonitorSocketApplication ALL_SERVERS_APPLICATION = new ServerMonitorSocketApplication(true);
    private static final ServerMonitorSocketApplication ONE_SERVER_APPLICATION  = new ServerMonitorSocketApplication(false);
    private static final String                         ALL_SERVERS_URL_PATTERN = "/serverlist";
    private static final String                         ONE_SERVER_URL_PATTERN  = "/servermonitor/*";
    private final boolean allServers;

    private ServerMonitorSocketApplication(boolean allServers)
    {
        this.allServers = allServers;
        TIMER.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                for (WebSocket socket : getWebSockets()) socket.sendPing("ping".getBytes());
            }
        }, SOCKET_PING_TIME, SOCKET_PING_TIME);
    }

    public static void sendUpdateToAll(Server server)
    {
        ALL_SERVERS_APPLICATION.doSendUpdateToAll(server);
        ONE_SERVER_APPLICATION.doSendUpdateToAll(server);
    }

    public static void register()
    {
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, ALL_SERVERS_URL_PATTERN, ALL_SERVERS_APPLICATION);
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, ONE_SERVER_URL_PATTERN, ONE_SERVER_APPLICATION);
    }

    @Override
    public void onConnect(WebSocket socket)
    {
        Session session = DefaultSessionManager.instance().getSession(null, ((DefaultWebSocket) socket).getUpgradeRequest().getRequestedSessionId());
        if (session == null)
        {
            WebSocketHelper.sendError(socket, "No valid session.");
            socket.close();
            return;
        }
        ((DefaultWebSocket) socket).getUpgradeRequest().setAttribute(USER, session.getAttribute(USER));

        if (allServers)
        {
            for (Server server : SETTINGS.getServers())
            {
                if (server.canUserControl((User) session.getAttribute(USER))) WebSocketHelper.sendData(socket, getData(server));
            }
        }
        else
        {
            String serverName = ((DefaultWebSocket) socket).getUpgradeRequest().getPathInfo();
            if (Strings.isNullOrEmpty(serverName) || Strings.isNullOrEmpty(serverName.substring(1)))
            {
                WebSocketHelper.sendError(socket, "No valid server.");
                socket.close();
                return;
            }
            Server server = Settings.getServerByName(serverName.substring(1));
            if (server == null)
            {
                WebSocketHelper.sendError(socket, "No valid server.");
                socket.close();
                return;
            }
            else if (!server.canUserControl((User) session.getAttribute(USER)))
            {
                WebSocketHelper.sendError(socket, "You have no rights to this server.");
                socket.close();
                return;
            }
            WebSocketHelper.sendData(socket, getData(server));
            ((DefaultWebSocket) socket).getUpgradeRequest().setAttribute(SERVER, server);
        }

        // Add socket to the list of sockets
        super.onConnect(socket);
    }

    public void doSendUpdateToAll(Server server)
    {
        for (WebSocket socket : getWebSockets())
        {
            if (!allServers && ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER) != server) continue;
            if (server.canUserControl((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER))) WebSocketHelper.sendData(socket, getData(server));
        }
    }

    public JsonObject getData(Server server)
    {
        JsonObject root = new JsonObject();

        root.addProperty("id", server.getID());
        root.addProperty("displayAddress", server.getDisplayAddress());
        root.addProperty("onlinePlayers", server.getOnlinePlayers());
        root.addProperty("playerList", JOINER_COMMA_SPACE.join(server.getPlayerList()));
        root.addProperty("slots", server.getSlots());
        root.addProperty("mapName", server.getMapName());
        root.addProperty("version", server.getVersion());
        root.addProperty("size", server.getDiskspaceUse()[2] + " MB");
        root.addProperty("motd", server.getMotd());
        root.addProperty("online", server.getOnline());
        root.addProperty("owner", server.getOwner());
        root.addProperty("gameMode", server.getGameMode());
        root.addProperty("plugins", server.getPlugins());
        root.addProperty("gameID", server.getGameID());
        root.addProperty("port_server_available", Helper.isPortAvailable(server.getIP(), server.getServerPort()));
        JsonObject object = new JsonObject();
        object.addProperty("server", server.getDiskspaceUse()[0]);
        object.addProperty("backups", server.getDiskspaceUse()[1]);
        object.addProperty("total", server.getDiskspaceUse()[2]);
        root.add("diskspace", object);
        root.add("coOwners", GSON.toJsonTree(server.getCoOwners()));
        root.add("admins", GSON.toJsonTree(server.getAdmins()));

        return root;
    }
}
