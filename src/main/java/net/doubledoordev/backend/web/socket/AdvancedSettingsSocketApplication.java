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
import net.doubledoordev.backend.server.JvmData;
import net.doubledoordev.backend.server.RestartingInfo;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.IUpdateFromJson;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.WebSocketHelper;
import net.doubledoordev.backend.util.exceptions.AuthenticationException;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.function.Function;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class AdvancedSettingsSocketApplication extends ServerWebSocketApplication
{
    private static final Map<String, Data> DATA_TYPES = new HashMap<>();

    static
    {
        DATA_TYPES.put(RESTARTING_INFO, new Data<>(AdvancedSettingsSocketApplication::getRestartingInfo));
        DATA_TYPES.put(JVM_DATA, new Data<>(AdvancedSettingsSocketApplication::getJvmData));
    }

    private static JvmData getJvmData(Server server)
    {
        return server.getJvmData();
    }

    private static RestartingInfo getRestartingInfo(Server server)
    {
        return server.getRestartingInfo();
    }

    private static final AdvancedSettingsSocketApplication APPLICATION = new AdvancedSettingsSocketApplication();
    private static final String URL_PATTERN = "/advancedsettings/*";

    private AdvancedSettingsSocketApplication()
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

    public static void register()
    {
        WebSocketEngine.getEngine().register(SOCKET_CONTEXT, URL_PATTERN, APPLICATION);
    }

    @Override
    public void onConnect(WebSocket socket)
    {
        super.onConnect(socket);

        Server server = (Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER);
        if (server.isCoOwner((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER)))
        {
            try
            {
                WebSocketHelper.sendData(socket, getData(server));
            }
            catch (Exception e)
            {
                WebSocketHelper.sendError(socket, e);
            }
        }
        else
        {
            WebSocketHelper.sendError(socket, new AuthenticationException());
            socket.close();
        }
    }

    @Override
    public void onMessage(WebSocket socket, String text)
    {
        Server server = (Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER);
        JsonObject object = JSONPARSER.parse(text).getAsJsonObject();
        DATA_TYPES.forEach((key, data) -> {
            if (!object.has(key)) return;
            data.set(server, object.getAsJsonObject(key));
        });
        doSendUpdateToAll(server);
        Settings.save();
    }

    private void doSendUpdateToAll(Server server)
    {
        for (WebSocket socket : getWebSockets())
        {
            if (((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER) != server) continue;
            if (!server.canUserControl((User) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(USER))) continue;
            try
            {
                WebSocketHelper.sendData(socket, getData(server));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                break;
            }
        }
    }

    private JsonObject getData(Server server) throws Exception
    {
        JsonObject object = new JsonObject();
        DATA_TYPES.forEach((s, data) -> object.add(s, GSON.toJsonTree(data.get(server))));
        return object;
    }

    private static class Data<T extends IUpdateFromJson>
    {
        private final Function<Server, T> getter;

        private Data(Function<Server, T> getter)
        {
            this.getter = getter;
        }

        private T get(Server s)
        {
            return getter.apply(s);
        }

        public void set(Server s, JsonObject jsonObject)
        {
            get(s).updateFrom(jsonObject);
        }
    }
}
