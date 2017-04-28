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

import com.google.gson.annotations.Expose;

/**
 * @author Dries007
 */
public class JvmData
{
    @Expose
    public int ramMin = 1024;
    @Expose
    public int ramMax = 2048;
    @Expose
    public int permGen = 128;
    @Expose
    public String extraJavaParameters = "";
    @Expose
    public String extraMCParameters = "";
    @Expose
    public String jarName = "minecraft_server.jar";
}
