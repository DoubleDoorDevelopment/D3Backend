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

package net.doubledoordev.backend.util;

import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.exceptions.OutOfPortsException;

import java.util.HashSet;

/**
 * Used to make sure all port assigned are within a range
 *
 * @author Dries007
 */
public class PortRange
{
    public int min = 25500;
    public int max = 25600;

    public int getNextAvailablePort(int ignored) throws OutOfPortsException
    {
        HashSet<Integer> usedPorts = new HashSet<>();
        for (Server server : Settings.SETTINGS.getServers())
        {
            usedPorts.add(server.getServerPort());
        }
        for (int port = min; port < max; port++)
        {
            if (!usedPorts.contains(port) && port != ignored) return port;
        }
        throw new OutOfPortsException();
    }

    public int getNextAvailablePort() throws OutOfPortsException
    {
        return getNextAvailablePort(-1);
    }
}
