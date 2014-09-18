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

package net.doubledoordev.backend.util;

import net.doubledoordev.backend.Main;
import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.doubledoordev.backend.util.Constants.CONFIG_FILE;
import static net.doubledoordev.backend.util.Constants.GSON;

/**
 * Global settings
 *
 * @author Dries007
 */
@SuppressWarnings("ALL")
public class Settings
{
    public static final Settings SETTINGS;

    static
    {
        Settings SETTINGS1;
        try
        {
            SETTINGS1 = GSON.fromJson(new FileReader(CONFIG_FILE), Settings.class);
        }
        catch (FileNotFoundException e)
        {
            SETTINGS1 = new Settings();
        }
        SETTINGS = SETTINGS1;
    }

    private String hostname = "localhost";
    private int port = 80;
    private boolean useJava8 = false;
    private List<Server> servers = new ArrayList<>();

    private Settings()
    {
    }

    public static Server getServerByName(String name)
    {
        if (name == null) throw new IllegalArgumentException(String.format("Server name %s is invalid.", name));
        for (Server server : Settings.SETTINGS.servers) if (server.getName().equalsIgnoreCase(name)) return server;
        throw new IllegalArgumentException(String.format("Server name %s is invalid.", name));
    }

    public String getHostname()
    {
        return hostname;
    }

    public int getPort()
    {
        return port;
    }

    public boolean useJava8()
    {
        return useJava8;
    }

    public List<Server> getServers()
    {
        return servers;
    }

    public void save()
    {
        try
        {
            FileUtils.writeStringToFile(CONFIG_FILE, GSON.toJson(this));
        }
        catch (IOException e)
        {
            Main.LOGGER.error("Error saving the config file...", e);
        }
    }
}
