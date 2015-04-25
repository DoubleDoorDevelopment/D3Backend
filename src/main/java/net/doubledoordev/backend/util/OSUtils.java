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

/**
 * In here because I want java finding to be a thing.
 *
 * @author FTB team
 */
public class OSUtils
{
    /**
     * Used to get the current operating system
     *
     * @return OS enum representing current operating system
     */
    public static OS getCurrentOS()
    {
        String osString = System.getProperty("os.name").toLowerCase();
        if (osString.contains("win"))
        {
            return OS.WINDOWS;
        }
        else if (osString.contains("nix") || osString.contains("nux"))
        {
            return OS.UNIX;
        }
        else if (osString.contains("mac"))
        {
            return OS.MACOSX;
        }
        else
        {
            return OS.OTHER;
        }
    }

    /**
     * Used to check if Windows is 64-bit
     *
     * @return true if 64-bit Windows
     */
    public static boolean is64BitWindows()
    {
        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
        String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
        return (arch.endsWith("64") || (wow64Arch != null && wow64Arch.endsWith("64")));
    }

    public static enum OS
    {
        WINDOWS, UNIX, MACOSX, OTHER,
    }
}
