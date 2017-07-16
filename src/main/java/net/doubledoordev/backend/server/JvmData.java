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

package net.doubledoordev.backend.server;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import net.doubledoordev.backend.util.IUpdateFromJson;

/**
 * @author Dries007
 */
public class JvmData implements IUpdateFromJson
{
    @Expose
    public int ramMin = 512;
    @Expose
    public int ramMax = 2048;
    @Expose
    public String extraJavaParameters = "";
    @Expose
    public String extraMCParameters = "";
    @Expose
    public String jarName = "minecraft_server.jar";

    @Override
    public void updateFrom(JsonObject json)
    {
        if (json.has("ramMin")) ramMin = json.get("ramMin").getAsInt();
        if (json.has("ramMax")) ramMax = json.get("ramMax").getAsInt();
        if (json.has("extraJavaParameters")) extraJavaParameters = json.get("extraJavaParameters").getAsString();
        if (json.has("extraMCParameters")) extraMCParameters = json.get("extraMCParameters").getAsString();
        if (json.has("jarName")) jarName = json.get("jarName").getAsString();
    }
}
