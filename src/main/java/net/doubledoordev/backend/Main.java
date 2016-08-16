/*
 * D3Backend
 * Copyright (C) 2015 - 2016  Dries007 & Double Door Development
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

package net.doubledoordev.backend;

import net.doubledoordev.backend.commands.CommandHandler;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Cache;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.web.http.FreemarkerHandler;
import net.doubledoordev.backend.web.http.ServerFileHandler;
import net.doubledoordev.backend.web.socket.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.websockets.WebSocketAddOn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.UUID;

import static net.doubledoordev.backend.util.Constants.*;
import static net.doubledoordev.backend.util.Settings.SETTINGS;

/**
 * @author Dries007
 */
public class Main
{
    public static final long STARTTIME = System.currentTimeMillis();
    public static final Logger LOGGER = LogManager.getLogger(Main.class.getSimpleName());
    public static final String build, version;
    public static String adminKey;
    public static boolean running = true;
    public static boolean debug = false;
    public static boolean safe = false;

    static
    {
        Properties properties = new Properties();
        try
        {
            properties.load(Main.class.getResourceAsStream("/properties.properties"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        build = properties.getProperty("build");
        version = properties.getProperty("version");
    }

    private static FreemarkerHandler freemarkerHandler;

    public static FreemarkerHandler getFreemarkerHandler()
    {
        return freemarkerHandler;
    }

    private Main()
    {

    }

    private static SSLEngineConfigurator createSslConfiguration() throws IOException
    {
        // Initialize SSLContext configuration
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        if (Strings.isNotBlank(SETTINGS.certificatePath))
        {
            sslContextConfig.setKeyStoreBytes(FileUtils.readFileToByteArray(new File(SETTINGS.certificatePath)));
            sslContextConfig.setKeyStorePass(SETTINGS.certificatePass);
        }

        // Create SSLEngine configurator
        return new SSLEngineConfigurator(sslContextConfig.createSSLContext(), false, false, false);
    }

    public static void main(String[] args) throws Exception
    {
        System.setProperty("file.encoding", "UTF-8");
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, null);

        for (String arg : args)
        {
            if (arg.equalsIgnoreCase("debug")) debug = true;
            if (arg.equalsIgnoreCase("safe")) safe = true;
        }

        if (debug) LOGGER.info("DEBUG MODE");
        if (safe) LOGGER.info("SAFE MODE");

        // todo: get JDK and classload tools.jar + native library; if not jdk: disable warmroast
        LOGGER.info("\n\n    D3Backend  Copyright (C) 2015 - 2016  Dries007 & Double Door Development\n" +
                "    This program comes with ABSOLUTELY NO WARRANTY;\n" +
                "    This is free software, and you are welcome to redistribute it under certain conditions;\n" +
                "    Type `license' for details.\n\n");

        LOGGER.info("System Properties");
        Properties properties = System.getProperties();
        for (Object key : properties.keySet()) LOGGER.info("-   {} = {}", key, properties.get(key));

        LOGGER.info("Making necessary folders...");
        mkdirs();
        LOGGER.info("Starting webserver...");

        final HttpServer webserver = new HttpServer();
        final ServerConfiguration config = webserver.getServerConfiguration();

        // Html stuff
        freemarkerHandler = new FreemarkerHandler(Main.class, TEMPLATES_PATH);
        config.addHttpHandler(freemarkerHandler);
        config.setDefaultErrorPageGenerator(freemarkerHandler);
        config.addHttpHandler(new CLStaticHttpHandler(Main.class.getClassLoader(), STATIC_PATH), STATIC_PATH);
        config.addHttpHandler(new ServerFileHandler(P2S_PATH), P2S_PATH);
        config.addHttpHandler(new ServerFileHandler(), RAW_PATH);

        // Socket stuff
        ServerMonitorSocketApplication.register();
        ServerControlSocketApplication.register();
        ServerPropertiesSocketApplication.register();
        FileManagerSocketApplication.register();
        ServerconsoleSocketApplication.register();
        ConsoleSocketApplication.register();
        AdvancedSettingsSocketApplication.register();
        UsersSocketApplication.register();
        FileMonitorSocketApplication.register();
        WorldManagerSocketApplication.register();

        final NetworkListener networkListener = new NetworkListener("listener", Strings.isBlank(SETTINGS.hostname) ? NetworkListener.DEFAULT_NETWORK_HOST : SETTINGS.hostname, Strings.isNotBlank(SETTINGS.certificatePath) ? SETTINGS.portHTTPS : SETTINGS.portHTTP);
        if (Strings.isNotBlank(SETTINGS.certificatePath))
        {
            networkListener.setSecure(true);
            networkListener.setSSLEngineConfig(createSslConfiguration());
            webserver.addListener(new NetworkListener("redirect-listener", Strings.isBlank(SETTINGS.hostname) ? NetworkListener.DEFAULT_NETWORK_HOST : SETTINGS.hostname, SETTINGS.portHTTP));
        }
        webserver.addListener(networkListener);
        networkListener.registerAddOn(new WebSocketAddOn());
        webserver.start();

        LOGGER.info("Setting up caching...");
        Cache.init();

        if (SETTINGS.users.isEmpty())
        {
            adminKey = UUID.randomUUID().toString();
            LOGGER.warn("Your userlist is empty.");
            LOGGER.warn("Make a new account and use the special admin token in the '2 + 2 = ?' field.");
            LOGGER.warn("You can only use this key once. It will be regenerated if the userlist is empty when the backend starts.");
            LOGGER.warn("Admin token: " + adminKey);
        }

        LOGGER.info("Use the help command for help.");

        CommandHandler.init();
        for (Server server : SETTINGS.servers.values())
        {
            try
            {
                server.init();
                if (!safe && server.getRestartingInfo().autoStart) server.startServer();
            }
            catch (Exception ignored)
            {
                LOGGER.catching(ignored);
                ignored.printStackTrace();
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void mkdirs()
    {
        Constants.SERVERS.mkdir();
    }

    public static synchronized void shutdown()
    {
        running = false;
        Settings.save();
        Cache.init();
        LOGGER.info("Attempting graceful shutdown of all servers...");
        for (final Server server : Settings.SETTINGS.servers.values())
        {
            if (server.getOnline())
            {
                LOGGER.info("Server " + server.getID() + " is still online.");
                try
                {
                    try
                    {
                        server.stopServer(NAME + " shutdown!");
                    }
                    catch (Exception e)
                    {
                        server.getProcess().destroy();
                    }

                    LOGGER.info("Waiting for server " + server.getID() + " to shutdown...");
                    server.getProcess().waitFor();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    LOGGER.info("Something went wrong while waiting for server " + server.getID(), e);
                }
            }
        }
        LOGGER.info("Bye!");
        Runtime.getRuntime().exit(0);
    }
}
