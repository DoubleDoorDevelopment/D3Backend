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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Helper;
import net.doubledoordev.backend.util.WebSocketHelper;
import net.doubledoordev.backend.util.methodCaller.IMethodCaller;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * Short term sockets
 * Get 1 command, and send 1 response back.
 *
 * @author Dries007
 */
public class ServerControlSocketApplication extends ServerWebSocketApplication
{
    private static final ServerControlSocketApplication APPLICATION = new ServerControlSocketApplication();
    private static final String URL_PATTERN = "/servercmd/*";

    private ServerControlSocketApplication()
    {
    }

    public static void register()
    {
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, URL_PATTERN, APPLICATION);
    }

    @Override
    public void onMessage(WebSocket socket, String text)
    {
        Server server = (Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER);
        if (!server.canUserControl((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER)))
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
            IMethodCaller methodCaller = Helper.invokeWithRefectionMagic(socket, server, name, args);
            if (methodCaller == null)
            {
                WebSocketHelper.sendOk(socket);
                socket.close();
            }
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
        {
            WebSocketHelper.sendError(socket, e);
            socket.close();
        }
    }
}
