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

import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.methodCaller.IMethodCaller;
import net.doubledoordev.backend.util.methodCaller.WebSocketCaller;
import org.apache.logging.log4j.util.Strings;
import org.glassfish.grizzly.websockets.WebSocket;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.doubledoordev.backend.util.Constants.GSON;
import static net.doubledoordev.backend.util.Constants.JSONPARSER;
import static net.doubledoordev.backend.util.Settings.SETTINGS;

/**
 * Public static helper methods
 * Passed to template engine
 *
 * @author Dries007
 */
@SuppressWarnings("UnusedDeclaration")
public class Helper
{
    private static final Map<String, String> UUID_USERNMAME_MAP = new HashMap<>();
    private static final SimpleDateFormat BAN_SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final char[] symbols = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int MAX_REDIRECTS = 25;

    private Helper()
    {
    }

    public static String getStackTrace(final Throwable throwable)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Checks to see if a port/hostname combo is available through opening a socked and closing it again
     *
     * @param hostname the hostname, if null this is bypassed
     * @param port     the port to check
     * @return true if available
     */
    public static boolean isPortAvailable(String hostname, int port)
    {
        if (port == -1) return false;
        try
        {
            ServerSocket socket = new ServerSocket();
            socket.bind(hostname == null || hostname.length() == 0 ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port));
            socket.close();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static String randomString(int length)
    {
        return new String(randomCharArray(length));
    }

    public static char[] randomCharArray(int length)
    {
        if (length < 1) throw new IllegalArgumentException("length < 1: " + length);

        final char[] buf = new char[length];

        for (int idx = 0; idx < buf.length; ++idx) buf[idx] = symbols[Constants.RANDOM.nextInt(symbols.length)];
        return buf;
    }

    /**
     * @return set of all MC versions we can grab from mojang
     */
    public static Collection<String> getAllMCVersions()
    {
        return Cache.getMcVersions().keySet();
    }

    public static String getForgeVersionJson()
    {
        return GSON.toJson(Cache.getForgeVersions());
    }

    public static int getTotalRamUsed()
    {
        int total = 0;
        for (Server server : Settings.SETTINGS.getOnlineServers()) total += server.getJvmData().ramMax;
        return total;
    }

    public static int getTotalDiskspaceUsed()
    {
        int total = 0;
        for (Server server : Settings.SETTINGS.getServers()) total += server.getDiskspaceUse()[2];
        return total;
    }

    public static String getNowInBanFormat()
    {
        return BAN_SIMPLE_DATE_FORMAT.format(new Date());
    }

    public static Tag<?> readRawNBT(File file, boolean compressed)
    {
        Tag<?> tag = null;
        try
        {
            InputStream is = new FileInputStream(file);
            NBTInputStream ns = new NBTInputStream(is, compressed);
            try
            {
                tag = ns.readTag();
            }
            finally
            {
                try
                {
                    ns.close();
                }
                catch (IOException ignored)
                {

                }
            }
        }
        catch (Exception ignored)
        {

        }
        return tag;
    }

    public static void writeRawNBT(File file, boolean compressed, Tag<?> tag)
    {
        try
        {
            OutputStream is = new FileOutputStream(file);
            NBTOutputStream ns = new NBTOutputStream(is, compressed);
            try
            {
                ns.writeTag(tag);
            }
            finally
            {
                try
                {
                    ns.close();
                }
                catch (IOException ignored)
                {

                }
            }
        }
        catch (Exception ignored)
        {

        }
    }

    public static String getUsernameFromUUID(String uuid)
    {
        if (uuid.endsWith(".dat")) uuid = uuid.substring(0, uuid.lastIndexOf('.'));
        uuid = uuid.replace("-", "");
        if (uuid.length() != 32) return null;
        if (!UUID_USERNMAME_MAP.containsKey(uuid))
        {
            // This variable is used so it will only ever check any UUID once. Even if its name is null.
            String name = null;
            try
            {
                URL url = new URL(String.format("https://api.mojang.com/user/profiles/%s/names", uuid));
                URLConnection uc = url.openConnection();
                uc.setUseCaches(false);
                uc.setDefaultUseCaches(false);
                uc.addRequestProperty("User-Agent", "minecraft");
                InputStream in = uc.getInputStream();
                name = JSONPARSER.parse(new InputStreamReader(in)).getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();
                in.close();
            }
            catch (Exception ignored)
            {

            }
            UUID_USERNMAME_MAP.put(uuid, name);
        }
        return UUID_USERNMAME_MAP.get(uuid);
    }

    public static boolean usingHttps()
    {
        return !(Strings.isBlank(SETTINGS.certificatePath) || Strings.isBlank(SETTINGS.certificatePass));
    }

    public static boolean hasUpdate()
    {
        return Cache.hasUpdate();
    }

    public static String getUpdateVersion()
    {
        return Cache.getUpdateVersion();
    }

    public static int getGlobalPlayers()
    {
        int p = 0;
        for (Server server : SETTINGS.getOnlineServers())
        {
            p += server.getOnlinePlayers();
        }
        return p;
    }

    /**
     * Default arguments: "%2d days, ", "%2d hours, ", "%2d min and", "%2 sec"
     */
    public static String getOnlineTime(long startTime, String dayString, String hoursString, String minuteString, String secondsString)
    {
        StringBuilder sb = new StringBuilder(30);
        long time = System.currentTimeMillis() - startTime;
        boolean alwaysShowNext = false;
        if (Strings.isNotBlank(dayString) && time > 1000 * 60 * 60 * 24)
        {
            alwaysShowNext = true;
            sb.append(String.format(dayString, time / (1000 * 60 * 60 * 24)));
            time %= 1000 * 60 * 60 * 24;
        }
        if (Strings.isNotBlank(hoursString) && (alwaysShowNext || time > 1000 * 60 * 60))
        {
            alwaysShowNext = true;
            sb.append(String.format(hoursString, time / (1000 * 60 * 60)));
            time %= 1000 * 60 * 60;
        }
        if (Strings.isNotBlank(minuteString) && (alwaysShowNext || time > 1000 * 60))
        {
            sb.append(String.format(minuteString, time / (1000 * 60)));
            time %= 1000 * 60;
        }
        sb.append(String.format(secondsString, time / (1000)));
        return sb.toString();
    }

    public static String getOnlineTime(String dayString, String hoursString, String minuteString, String secondsString)
    {
        return getOnlineTime(Main.STARTTIME, dayString, hoursString, minuteString, secondsString);
    }

    public static String getReadOnlyProperties()
    {
        JsonArray array = new JsonArray();

        if (SETTINGS.fixedPorts) array.add(new JsonPrimitive(Server.SERVER_PORT));
        if (SETTINGS.fixedIP) array.add(new JsonPrimitive(Server.SERVER_IP));

        array.add(new JsonPrimitive(Server.QUERY_PORT));
        array.add(new JsonPrimitive(Server.QUERY_ENABLE));

        return array.toString();
    }

    public static IMethodCaller invokeWithRefectionMagic(WebSocket caller, Object instance, String methodName, ArrayList<String> args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        for (java.lang.reflect.Method method : instance.getClass().getDeclaredMethods())
        {
            if (!method.getName().equalsIgnoreCase(methodName)) continue; // Name match
            boolean userMethodCaller = method.getParameterTypes().length != 0 && method.getParameterTypes()[0].isAssignableFrom(IMethodCaller.class); // See if first type is IMethodCaller
            int size = args.size() + (userMethodCaller ? 1 : 0); // deferments wanted parameter length
            if (method.getParameterTypes().length == size) // parameter length match
            {
                try
                {
                    Object parms[] = new Object[size];
                    if (userMethodCaller) parms[0] = new WebSocketCaller(caller);
                    for (int i = userMethodCaller ? 1 : 0; i < method.getParameterTypes().length; i++)
                    {
                        parms[i] = TypeHellhole.convert(method.getParameterTypes()[i], args.get(i - (userMethodCaller ? 1 : 0)));
                    }
                    method.invoke(instance, parms);
                    return userMethodCaller ? (IMethodCaller) parms[0] : null;
                }
                catch (ClassCastException ignored)
                {
                    ignored.printStackTrace();
                }
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    public static String getVersionString()
    {
        return String.format("v%s - build #%s", Main.version, Main.build);
    }

    public static String formatDate(long time)
    {
        return SIMPLE_DATE_FORMAT.format(time);
    }

    public static String stripColor(String txt)
    {
        return txt.replaceAll("(?i)\u00a7[0-9a-fk-or]", "");
    }

    public static URL getFinalURL(String url) throws IOException
    {
        for (int i = 0; i < MAX_REDIRECTS; i++)
        {
            HttpURLConnection con = null;
            try
            {
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setInstanceFollowRedirects(false);
                con.connect();
                if (con.getHeaderField("Location") == null)
                {
                    return new URL(url.replace("?cookieTest=1", ""));
                }
                url = con.getHeaderField("Location");
            }
            catch (IOException e)
            {
                return new URL(url.replace("?cookieTest=1", ""));
            }
            finally
            {
                if (con != null) con.disconnect();
            }
        }
        throw new IOException("Redirect limit (" + MAX_REDIRECTS + ") exceeded on url: " + url);
    }

    public static void doWebMethodCall(WebSocket socket, String text, Object o)
    {
        try
        {
            JsonObject object = JSONPARSER.parse(text).getAsJsonObject();
            String name = object.get("method").getAsString();
            ArrayList<String> args = new ArrayList<>();
            if (object.has("args")) for (JsonElement arg : object.getAsJsonArray("args")) args.add(arg.getAsString());
            IMethodCaller methodCaller = Helper.invokeWithRefectionMagic(socket, o, name, args);
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
