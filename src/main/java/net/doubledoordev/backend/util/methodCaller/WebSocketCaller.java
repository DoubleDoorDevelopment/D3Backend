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

package net.doubledoordev.backend.util.methodCaller;

import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.util.WebSocketHelper;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;

import static net.doubledoordev.backend.util.Constants.USER;

/**
 * @author Dries007
 */
public class WebSocketCaller implements IMethodCaller
{
    final WebSocket socket;

    public WebSocketCaller(WebSocket socket)
    {
        this.socket = socket;
    }

    @Override
    public User getUser()
    {
        return (User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER);
    }

    @Override
    public void sendOK()
    {
        WebSocketHelper.sendOk(socket);
    }

    @Override
    public void sendMessage(String message)
    {
        if (socket.isConnected()) WebSocketHelper.sendData(socket, message);
    }

//    @Override
//    public void sendProgress(float progress)
//    {
//        WebSocketHelper.sendData(socket, progress);
//    }

    @Override
    public void sendError(String message)
    {
        if (socket.isConnected()) WebSocketHelper.sendError(socket, message);
    }

    @Override
    public void sendDone()
    {
        WebSocketHelper.sendData(socket, "done");
        if (socket.isConnected()) socket.close();
    }
}
