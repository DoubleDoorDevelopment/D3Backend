/*
 * D3Backend
 * Copyright (C) 2015 - 2017  Dries007 & Double Door Development
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

import com.google.gson.JsonObject;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.WebSocketHelper;
import net.doubledoordev.backend.util.methodCaller.WebSocketCaller;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.TimerTask;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class ServerPropertiesSocketApplication extends ServerWebSocketApplication
{
    private static final ServerPropertiesSocketApplication APPLICATION = new ServerPropertiesSocketApplication();
    private static final String URL_PATTERN = "/serverproperties/*";

    private ServerPropertiesSocketApplication()
    {
        TIMER_NETWORK.scheduleAtFixedRate(new TimerTask()
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
        APPLICATION.doSendUpdateToAll(server);
    }

    public static void register()
    {
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, URL_PATTERN, APPLICATION);
    }

    @Override
    public void onConnect(WebSocket socket)
    {
        super.onConnect(socket);

        WebSocketHelper.sendData(socket, getData((Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER)));
    }

    @Override
    public void onMessage(WebSocket socket, String text)
    {
        Server server = (Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER);
        String[] split = text.split("=", 2);
        try
        {
            server.setProperty(new WebSocketCaller(socket), split[0], split[1]);
        }
        catch (Exception e)
        {
            WebSocketHelper.sendError(socket, e);
        }
        doSendUpdateToAll(server);
    }

    private void doSendUpdateToAll(Server server)
    {
        for (WebSocket socket : getWebSockets())
        {
            if (((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER) != server) continue;
            if (server.canUserControl((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER))) WebSocketHelper.sendData(socket, getData(server));
        }
    }

    public JsonObject getData(Server server)
    {
        JsonObject object = new JsonObject();

        for (Object key : server.getProperties().keySet())
        {
            object.addProperty(key.toString(), server.getProperty(key.toString()));
        }

        return object;
    }
}
