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

import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.FileManager;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Helper;
import net.doubledoordev.backend.util.WebSocketHelper;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.TimerTask;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class FileManagerSocketApplication extends ServerWebSocketApplication
{
    private static final FileManagerSocketApplication APPLICATION = new FileManagerSocketApplication();
    private static final String URL_PATTERN = "/filemanager/*";

    private FileManagerSocketApplication()
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
        Helper.doWebMethodCall(socket, text, fileManager);
    }
}
