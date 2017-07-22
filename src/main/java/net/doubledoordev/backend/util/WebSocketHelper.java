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

package net.doubledoordev.backend.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.glassfish.grizzly.websockets.WebSocket;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class WebSocketHelper
{
    private WebSocketHelper()
    {
    }

    public static void sendError(WebSocket socket, String message)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, ERROR);
        root.addProperty(MESSAGE, message);

        socket.send(root.toString());
    }

    public static void sendData(WebSocket socket, String s)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, OK);
        root.addProperty(DATA, s);

        socket.send(root.toString());
    }

    public static void sendData(WebSocket socket, Boolean b)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, OK);
        root.addProperty(DATA, b);

        socket.send(root.toString());
    }

    public static void sendData(WebSocket socket, Character c)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, OK);
        root.addProperty(DATA, c);

        socket.send(root.toString());
    }

    public static void sendData(WebSocket socket, Number n)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, OK);
        root.addProperty(DATA, n);

        socket.send(root.toString());
    }

    public static void sendData(WebSocket socket, JsonElement s)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, OK);
        root.add(DATA, s);

        socket.send(root.toString());
    }

    public static void sendOk(WebSocket socket)
    {
        JsonObject root = new JsonObject();

        root.addProperty(STATUS, OK);

        socket.send(root.toString());
    }

    public static void sendError(WebSocket socket, Throwable e)
    {
        StringBuilder error = new StringBuilder();
        do
        {
            error.append('\n').append(e.getClass().getSimpleName());
            if (e.getMessage() != null) error.append(": ").append(e.getMessage());
        } while ((e = e.getCause()) != null);
        WebSocketHelper.sendError(socket, error.substring(1));
        socket.close();
    }
}
