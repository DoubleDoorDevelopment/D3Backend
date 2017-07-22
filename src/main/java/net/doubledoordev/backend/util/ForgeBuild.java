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

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.doubledoordev.backend.util.Constants.FORGE_INSTALLER_URL;
import static net.doubledoordev.backend.util.Constants.FORGE_USER_AGENT;

/**
 * @author Dries007
 */
public class ForgeBuild
{
    public final String branch;
    public final int build;
    public final String version;
    public final String mcVersion;

    public final String networkId; // The maven version string
    public final String id;

    public ForgeBuild(JsonObject o)
    {
        branch = o.has("branch") && !o.get("branch").isJsonNull() ? o.get("branch").getAsString() : null;
        build = o.get("build").getAsInt();
        mcVersion = o.get("mcversion").getAsString();
        version = o.get("version").getAsString();

        StringBuilder sb = new StringBuilder().append(version);
        if (branch != null) sb = sb.append('-').append(branch);
        id = sb.toString();
        networkId = mcVersion + "-" + id;
    }

    @Override
    public String toString()
    {
        return id;
    }

    public boolean hasInstallerNetwork()
    {
        boolean found = false;
        try
        {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(FORGE_INSTALLER_URL.replace("%ID%", networkId)).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("User-Agent", FORGE_USER_AGENT);
            urlConnection.connect();
            found = urlConnection.getResponseCode() == 200;
            urlConnection.disconnect();
        }
        catch (IOException ignored)
        {
            // timeout, 404, ...
        }
        return found;
    }
}
