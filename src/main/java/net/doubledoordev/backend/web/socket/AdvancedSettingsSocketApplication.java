/*
 *     D3Backend
 *     Copyright (C) 2015  Dries007 & Double Door Development
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.doubledoordev.backend.web.socket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.JvmData;
import net.doubledoordev.backend.server.RestartingInfo;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.TypeHellhole;
import net.doubledoordev.backend.util.WebSocketHelper;
import net.doubledoordev.backend.util.exceptions.AuthenticationException;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class AdvancedSettingsSocketApplication extends ServerWebSocketApplication
{
    public static final HashMap<String, Data> DATA_TYPES = new HashMap<>();

    static
    {
        try
        {
            DATA_TYPES.put(RESTARTING_INFO, new Data(RestartingInfo.class, RESTARTING_INFO));
            DATA_TYPES.put(JVM_DATA, new Data(JvmData.class, JVM_DATA));
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static final AdvancedSettingsSocketApplication APPLICATION = new AdvancedSettingsSocketApplication();
    private static final String URL_PATTERN = "/advancedsettings/*";

    private AdvancedSettingsSocketApplication()
    {
        TIMER.scheduleAtFixedRate(new TimerTask()
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
        Main.LOGGER.info(text);

        Server server = (Server) ((DefaultWebSocket) socket).getUpgradeRequest().getAttribute(SERVER);
        JsonObject object = JSONPARSER.parse(text).getAsJsonObject();
        for (String key : DATA_TYPES.keySet())
        {
            if (object.has(key))
            {
                try
                {
                    DATA_TYPES.get(key).setValues(server, object.getAsJsonObject(key));
                }
                catch (Exception e)
                {
                    WebSocketHelper.sendError(socket, e);
                }
            }
        }
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

    public JsonObject getData(Server server) throws Exception
    {
        JsonObject object = new JsonObject();

        for (String key : DATA_TYPES.keySet())
        {
            object.add(key, GSON.toJsonTree(DATA_TYPES.get(key).getter.invoke(server)));
        }

        return object;
    }

    public static class Data
    {
        public final Class clazz;
        public final Method getter;

        public Data(Class clazz, String getterName) throws NoSuchMethodException
        {
            this.clazz = clazz;
            Method m;
            try
            {
                m = Server.class.getDeclaredMethod(getterName);
            }
            catch (NoSuchMethodException ignored)
            {
                m = Server.class.getDeclaredMethod("get" + getterName);
            }
            this.getter = m;
        }

        public void setValues(Server server, JsonObject data) throws Exception
        {
            Object object = getter.invoke(server);
            for (Map.Entry<String, JsonElement> entry : data.entrySet())
            {
                TypeHellhole.set(clazz.getDeclaredField(entry.getKey()), object, entry.getValue());
            }
        }
    }
}
