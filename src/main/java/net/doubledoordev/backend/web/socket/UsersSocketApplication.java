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

/**
 * @author Dries007
 */
public class UsersSocketApplication extends WebSocketApplication
{
    private static final UsersSocketApplication APPLICATION = new UsersSocketApplication();
    private static final String URL_PATTERN = "/users/*";

    private UsersSocketApplication()
    {
        ServerWebSocketApplication.TIMER_NETWORK.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                for (WebSocket socket : getWebSockets()) socket.sendPing("ping".getBytes());
            }
        }, ServerWebSocketApplication.SOCKET_PING_TIME, ServerWebSocketApplication.SOCKET_PING_TIME);
    }

    public static void register()
    {
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, URL_PATTERN, APPLICATION);
    }

    @Override
    public void onMessage(WebSocket socket, String text)
    {
        User user = (User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER);
        Helper.doWebMethodCall(socket, text, user);
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
        if (!((User) session.getAttribute(USER)).isAdmin())
        {
            WebSocketHelper.sendError(socket, "You are no admin.");
            socket.close();
            return;
        }
        String[] username = ((DefaultWebSocket) socket).getUpgradeRequest().getPathInfo().substring(1).split("/");
        if (Strings.isNullOrEmpty(username[0]) || Strings.isNullOrEmpty(username[0]))
        {
            WebSocketHelper.sendError(socket, "No valid user.");
            socket.close();
            return;
        }
        User user = Settings.getUserByName(username[0]);
        if (user == null)
        {
            WebSocketHelper.sendError(socket, "No valid user.");
            socket.close();
            return;
        }
        ((DefaultWebSocket) socket).getUpgradeRequest().setAttribute(USER, user);
        super.onConnect(socket);
    }
}
