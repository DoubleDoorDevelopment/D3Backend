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

/**
 * Adapted from https://stackoverflow.com/a/11024200
 *
 * @author alex from Stackoverflow
 */
public class Version implements Comparable<Version>
{
    public final String[] version;

    public Version(String version)
    {
        this.version = version.split("\\.");
    }

    private int safeParseInt(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException ignored)
        {
            return -1;
        }
    }

    @Override
    public int compareTo(Version that)
    {
        if (that == null) return 1;
        int length = Math.max(this.version.length, that.version.length);
        for (int i = 0; i < length; i++)
        {
            int thisPart = i < this.version.length ? safeParseInt(this.version[i]) : 0;
            int thatPart = i < that.version.length ? safeParseInt(that.version[i]) : 0;
            if (thisPart < thatPart) return -1;
            if (thisPart > thatPart) return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object that)
    {
        return this == that || that != null && this.getClass() == that.getClass() && this.compareTo((Version) that) == 0;
    }
}
