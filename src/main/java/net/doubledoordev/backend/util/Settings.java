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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import org.apache.commons.io.FileUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.doubledoordev.backend.Main.LOGGER;
import static net.doubledoordev.backend.util.Constants.*;

/**
 * Global settings
 *
 * @author Dries007
 */
@SuppressWarnings("ALL")
public class Settings
{
    public static final Settings SETTINGS = new Settings();

    static
    {
        JsonParser jsonParser = new JsonParser();
        try
        {
            JsonObject jsonElement = jsonParser.parse(new FileReader(CONFIG_FILE)).getAsJsonObject();
            SETTINGS.hostname = jsonElement.get("hostname").getAsString();
            SETTINGS.port = jsonElement.get("port").getAsInt();
            SETTINGS.useJava8 = jsonElement.get("useJava8").getAsBoolean();
            SETTINGS.fixedPorts = jsonElement.get("fixedPorts").getAsBoolean();
            SETTINGS.fixedIP = jsonElement.get("fixedIP").getAsBoolean();
            SETTINGS.portRange = GSON.fromJson(jsonElement.getAsJsonObject("portRange"), PortRange.class);

            if (SERVERS_FILE.exists()) Collections.addAll(SETTINGS.servers, GSON.fromJson(new FileReader(SERVERS_FILE), Server[].class));

            if (USERS_FILE.exists()) Collections.addAll(SETTINGS.users, GSON.fromJson(new FileReader(USERS_FILE), User[].class));
        }
        catch (Exception e)
        {
            // we don't care yet. TODO <<-
        }
    }
    public List<Server> servers = new ArrayList<>();
    public List<User> users = new ArrayList<>();

    public String hostname = "localhost";
    public int port = 80;
    public boolean useJava8 = false;
    public boolean fixedPorts = false;
    public boolean fixedIP = false;
    public PortRange portRange = new PortRange();

    private Settings()
    {
    }

    public static void save()
    {
        try
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("hostname", SETTINGS.hostname);
            jsonObject.addProperty("port", SETTINGS.port);
            jsonObject.addProperty("useJava8", SETTINGS.useJava8);
            jsonObject.addProperty("fixedPorts", SETTINGS.fixedPorts);
            jsonObject.addProperty("fixedIP", SETTINGS.fixedIP);
            jsonObject.add("portRange", GSON.toJsonTree(SETTINGS.portRange));
            FileUtils.writeStringToFile(CONFIG_FILE, GSON.toJson(jsonObject));

            FileUtils.writeStringToFile(SERVERS_FILE, GSON.toJson(SETTINGS.servers));
            FileUtils.writeStringToFile(USERS_FILE, GSON.toJson(SETTINGS.users));

            LOGGER.info("Saved settings.");
        }
        catch (IOException e)
        {
            LOGGER.error("Error saving the config file...", e);
        }
    }
}
