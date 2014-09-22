/*
 * Unless otherwise specified through the '@author' tag or the javadoc
 * at the top of the file or on a specific portion of the code the following license applies:
 *
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.backend.permissions;

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
    private String username, passhash;
    private int maxServers, maxRam;
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
            return true;
        }
        else return false;
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
            if (server.getOwner().equals(username) && server.getOnline()) leftover -= server.getRamMax();
        return leftover > 0 ? leftover : 0;
    }

    public int getServerCount()
    {
        int i = 0;
        for (Server server : Settings.SETTINGS.servers.values())
            if (server.getOwner().equals(username)) i++;
        return i;
    }
}
