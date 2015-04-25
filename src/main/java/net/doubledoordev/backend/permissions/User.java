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

package net.doubledoordev.backend.permissions;

import com.google.gson.annotations.Expose;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.PasswordHash;
import net.doubledoordev.backend.util.Settings;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * User object. Read from Json file with GSON
 *
 * @author Dries007
 */
public class User
{
    @Expose
    private String username, passhash;
    @Expose
    private int maxServers, maxRam, maxDiskspace = Settings.SETTINGS.defaultDiskspace;
    @Expose
    private Group group = Group.NORMAL;

    public User(String username, String passhash)
    {
        this.username = username;
        this.passhash = passhash;
    }

    public boolean verify(String password)
    {
        try
        {
            return PasswordHash.validatePassword(password, passhash);
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            // Hash algorithm doesn't work.
            throw new RuntimeException(e);
        }
    }

    public boolean updatePassword(String oldPass, String newPass)
    {
        if (verify(oldPass))
        {
            setPass(newPass);
            return true;
        }
        else return false;
    }

    public void setPass(String newPass)
    {
        try
        {
            passhash = PasswordHash.createHash(newPass);
            Settings.save();
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            // Hash algorithm doesn't work.
            throw new RuntimeException(e);
        }
    }

    public String getUsername()
    {
        return username;
    }

    public String getPasshash()
    {
        return passhash;
    }

    public Group getGroup()
    {
        return group;
    }

    public void setGroup(Group group)
    {
        this.group = group;
        Settings.save();
    }

    public void setGroup(String group)
    {
        setGroup(Group.valueOf(group));
    }

    public int getMaxServers()
    {
        return maxServers;
    }

    public void setMaxServers(int maxServers)
    {
        this.maxServers = maxServers;
        Settings.save();
    }

    public int getMaxRam()
    {
        return maxRam;
    }

    public void setMaxRam(int maxRam)
    {
        this.maxRam = maxRam;
        Settings.save();
    }

    public int getMaxRamLeft()
    {
        if (getMaxRam() == -1) return -1;
        int leftover = getMaxRam();
        for (Server server : Settings.SETTINGS.servers.values())
            if (server.getOwner().equals(username) && server.getOnline()) leftover -= server.getJvmData().ramMax;
        return leftover > 0 ? leftover : 0;
    }

    public int getServerCount()
    {
        int i = 0;
        for (Server server : Settings.SETTINGS.servers.values())
            if (server.getOwner().equals(username)) i++;
        return i;
    }

    public int getMaxDiskspace()
    {
        return maxDiskspace;
    }

    public void setMaxDiskspace(int maxDiskspace)
    {
        this.maxDiskspace = maxDiskspace;
        Settings.save();
    }

    public int getDiskspaceLeft()
    {
        if (getMaxDiskspace() == -1) return -1;
        int leftover = getMaxDiskspace();
        for (Server server : Settings.SETTINGS.servers.values())
            if (server.getOwner().equals(username)) leftover -= server.getDiskspaceUse()[2];
        return leftover > 0 ? leftover : 0;
    }

    public boolean isAdmin()
    {
        return this.getGroup() == Group.ADMIN;
    }

    public void delete()
    {
        Settings.SETTINGS.users.remove(this.getUsername().toLowerCase());
        Settings.save();
    }
}
