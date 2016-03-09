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

package net.doubledoordev.backend.server.query;

import com.google.common.base.Charsets;

import java.util.ArrayList;

/**
 * @author Ryan McCann
 */
public class QueryResponse
{
    private boolean fullstat;

    //for simple stat
    private String motd, gameMode, mapName;
    private int onlinePlayers, maxPlayers;
    private short port;
    private String hostname;

    //for full stat only
    private String gameID;
    private String version;
    private String plugins;
    private ArrayList<String> playerList;

    public QueryResponse(byte[] data, boolean fullstat)
    {
        this.fullstat = fullstat;

        data = ByteUtils.trim(data);
        byte[][] temp = ByteUtils.split(data);

        //		if(temp.length == 6) //short stat
        if (!fullstat)
        {
            motd = new String(ByteUtils.subarray(temp[0], 1, temp[0].length - 1), Charsets.ISO_8859_1);
            gameMode = new String(temp[1], Charsets.ISO_8859_1);
            mapName = new String(temp[2], Charsets.ISO_8859_1);
            onlinePlayers = Integer.parseInt(new String(temp[3], Charsets.ISO_8859_1));
            maxPlayers = Integer.parseInt(new String(temp[4], Charsets.ISO_8859_1));
            port = ByteUtils.bytesToShort(temp[5]);
            hostname = new String(ByteUtils.subarray(temp[5], 2, temp[5].length - 1), Charsets.ISO_8859_1);
        }
        else //full stat
        {
            motd = new String(temp[3], Charsets.ISO_8859_1);
            gameMode = new String(temp[5], Charsets.ISO_8859_1);
            mapName = new String(temp[13], Charsets.ISO_8859_1);
            onlinePlayers = Integer.parseInt(new String(temp[15], Charsets.ISO_8859_1));
            maxPlayers = Integer.parseInt(new String(temp[17], Charsets.ISO_8859_1));
            port = Short.parseShort(new String(temp[19], Charsets.ISO_8859_1));
            hostname = new String(temp[21], Charsets.ISO_8859_1);

            //only available with full stat:
            gameID = new String(temp[7], Charsets.ISO_8859_1);
            version = new String(temp[9], Charsets.ISO_8859_1);
            plugins = new String(temp[11], Charsets.ISO_8859_1);

            playerList = new ArrayList<>();
            for (int i = 25; i < temp.length; i++)
            {
                playerList.add(new String(temp[i], Charsets.ISO_8859_1));
            }
        }
    }

    public String toString()
    {
        String delimiter = ", ";
        StringBuilder str = new StringBuilder();
        str.append(motd);
        str.append(delimiter);
        str.append(gameMode);
        str.append(delimiter);
        str.append(mapName);
        str.append(delimiter);
        str.append(onlinePlayers);
        str.append(delimiter);
        str.append(maxPlayers);
        str.append(delimiter);
        str.append(port);
        str.append(delimiter);
        str.append(hostname);

        if (fullstat)
        {
            str.append(delimiter);
            str.append(gameID);
            str.append(delimiter);
            str.append(version);

            //plugins for non-vanilla (eg. Bukkit) servers
            if (plugins.length() > 0)
            {
                str.append(delimiter);
                str.append(plugins);
            }

            // player list
            str.append(delimiter);
            str.append("Players: ");
            str.append('[');
            for (String player : playerList)
            {
                str.append(player);
                if (playerList.indexOf(player) != playerList.size() - 1)
                {
                    str.append(',');
                }
            }
            str.append(']');
        }

        return str.toString();
    }

    public String getMotd()
    {
        return motd;
    }

    public String getGameMode()
    {
        return gameMode;
    }

    public String getMapName()
    {
        return mapName;
    }

    public int getOnlinePlayers()
    {
        return onlinePlayers;
    }

    public int getMaxPlayers()
    {
        return maxPlayers;
    }

    public String getPlugins()
    {
        return plugins;
    }

    public String getVersion()
    {
        return version;
    }

    public String getGameID()
    {
        return gameID;
    }

    /**
     * Returns an <code>ArrayList</code> of strings containing the connected players' usernames.
     * Note that this will return null for basic status requests.
     *
     * @return An <code>ArrayList</code> of player names
     */
    public ArrayList<String> getPlayerList()
    {
        return playerList;
    }
}
