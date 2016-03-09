/*
 * D3Backend
 * Copyright (C) 2015 - 2016  Dries007 & Double Door Development
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
    private static final ServerMonitorSocketApplication ONE_SERVER_APPLICATION = new ServerMonitorSocketApplication(false);
    private static final String ALL_SERVERS_URL_PATTERN = "/serverlist";
    private static final String ONE_SERVER_URL_PATTERN = "/servermonitor/*";
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
