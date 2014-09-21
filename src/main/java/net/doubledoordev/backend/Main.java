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

package net.doubledoordev.backend;

import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.server.rcon.RCon;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.webserver.NanoHTTPD;
import net.doubledoordev.backend.webserver.Webserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

import static net.doubledoordev.backend.util.Constants.NAME;

/**
 * @author Dries007
 */
public class Main
{
    public static final Logger LOGGER = LogManager.getLogger(Main.class.getSimpleName());

    private Main()
    {
    }

    public static void main(String[] args) throws Exception
    {
        LOGGER.info("+-------------------------------------------------------+");
        LOGGER.info("| ...................... Loading ...................... |");
        LOGGER.info("+-------------------------------------------------------+");
        LOGGER.info("Finding Java versions...");
        //noinspection ResultOfMethodCallIgnored
        Constants.JAVAPATH.length();

        LOGGER.info("Making necessary folders...");
        mkdirs();
        LOGGER.info("Starting webserver...");
        Webserver.WEBSERVER.start();

        LOGGER.info("+-------------------------------------------------------+");
        LOGGER.info("| Loading done. Press any key to terminate the program. |");
        LOGGER.info("+-------------------------------------------------------+");
        // Wait for user input.
        try
        {
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        }
        catch (Throwable ignored)
        {
            // Noop
        }

        shutdown();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void mkdirs()
    {
        Constants.SERVERS.mkdir();
    }

    public static synchronized void shutdown()
    {
        LOGGER.info("Attempting graceful shutdown of all servers...");
        for (final Server server : Settings.SETTINGS.servers.values())
        {
            if (server.getOnline())
            {
                LOGGER.info("Server " + server.getName() + " is still online.");
                try
                {
                    RCon rCon = server.getRCon();
                    try
                    {
                        for (String user : server.getPlayerList()) rCon.send("kick", user, NAME + " shutdown!");
                        rCon.stop();
                    }
                    catch (Exception e)
                    {
                        server.getProcess().destroy();
                    }

                    LOGGER.info("Waiting for server " + server.getName() + " to shutdown...");
                    server.getProcess().waitFor();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    LOGGER.info("Something went wrong while waiting for server " + server.getName(), e);
                }
            }
        }
        LOGGER.info("Bye!");
    }

    /**
     * Useful when debugging requests
     *
     * @param session
     * @param dataObject
     */
    public static void printdebug(NanoHTTPD.IHTTPSession session, HashMap<String, Object> dataObject)
    {
        LOGGER.debug("getParms: " + session.getParms());
        LOGGER.debug("getHeaders: " + session.getHeaders());
        LOGGER.debug("getUri: " + session.getUri());
        LOGGER.debug("getQueryParameterString: " + session.getQueryParameterString());
        LOGGER.debug("getMethod: " + session.getMethod());
        LOGGER.debug("getCookies: " + session.getCookies());
        LOGGER.debug("dataObject: " + dataObject);
        LOGGER.debug("-----================================-----");
    }
}
