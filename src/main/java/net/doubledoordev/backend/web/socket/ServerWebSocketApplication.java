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

import com.google.common.base.Strings;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.WebSocketHelper;
import org.glassfish.grizzly.http.server.DefaultSessionManager;
import org.glassfish.grizzly.http.server.Session;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import java.util.Timer;

import static net.doubledoordev.backend.util.Constants.SERVER;
import static net.doubledoordev.backend.util.Constants.USER;

/**
 * Used for keep-alive sockets, they get pinged regularly
 *
 * @author Dries007
 */
public abstract class ServerWebSocketApplication extends WebSocketApplication
{
    protected static final Timer TIMER_NETWORK = new Timer();
    protected static final long SOCKET_PING_TIME = 1000 * 50; // 50 seconds

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
        String[] serverName = ((DefaultWebSocket) socket).getUpgradeRequest().getPathInfo().substring(1).split("/");
        Server server = Settings.getServerByName(serverName[0]);
        if (Strings.isNullOrEmpty(serverName[0]) || server == null)
        {
            WebSocketHelper.sendError(socket, "No valid server.");
            socket.close();
            return;
        }
        if (!server.canUserControl((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER)))
        {
            WebSocketHelper.sendError(socket, "You have no rights to this server.");
            socket.close();
            return;
        }
        ((DefaultWebSocket) socket).getUpgradeRequest().setAttribute(SERVER, server);
        super.onConnect(socket);
    }
}
