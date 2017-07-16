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

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.doubledoordev.backend.util.winreg.JavaFinder;
import net.doubledoordev.backend.util.winreg.JavaInfo;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Timer;
import java.util.regex.Pattern;

/**
 * Constants!
 *
 * @author Dries007
 */
public class Constants
{
    /*
     * String constants
     */
    public static final String NAME = "D3 Backend";
    public static final String LOCALHOST = "localhost";
    public static final String WORLD = "world";
    public static final String SERVER = "server";
    public static final String FILE_MANAGER = "filemanager";
    public static final String WORLD_MANAGER = "worldmanager";
    public static final String FILE = "file";
    public static final String INDEX = "index";
    public static final String USER = "user";
    public static final String MESSAGE = "message";
    public static final String STATUS = "status";
    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String DATA = "data";
    public static final String DIM = "DIM";
    public static final String OVERWORLD = "Overworld";
    public static final String FORGE_USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)";
    public static final String RESTARTING_INFO = "RestartingInfo";
    public static final String JVM_DATA = "JvmData";
    public static final String TEMPLATE_EXTENSION = ".ftl";
    public static final String ERROR_TEMPLATE = "error.ftl";
    public static final String SLASH_STR = "/";
    public static final String STATIC_PATH = "/static/";
    public static final String TEMPLATES_PATH = "/templates/";
    public static final String P2S_PATH = "/pay2spawn/";
    public static final String RAW_PATH = "/raw/";
    public static final String SOCKET_CONTEXT = "/socket";
    public static final String LOGIN_URL = "/login";
    public static final String REGISTER_URL = "/register";
    public static final String NEWSERVER_URL = "/newserver";
    public static final String FILEMANAGER_URL = "/filemanager";
    public static final String SERVER_URL = "/server";
    public static final String SERVER_ID_URL = "/server?server=";
    public static final String FAVOTICON = "favicon.ico";
    public static final String MC_VERIONS_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String FORGE_VERIONS_URL = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json";
    public static final String FORGE_INSTALLER_URL = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/%ID%/forge-%ID%-installer.jar";
    public static final String VERSION_CHECKER_URL = "https://jenkins.dries007.net/job/D3Backend/api/json?tree=lastStableBuild[number,artifacts[*]]";

    public final static FilenameFilter NOT_DIM_FILTER = (dir, name) -> !name.startsWith(DIM);
    public final static FilenameFilter DIM_ONLY_FILTER = (dir, name) -> name.startsWith(DIM);

    public final static FilenameFilter ACCEPT_ALL_JAR_FILTER = (dir, name) -> FilenameUtils.getExtension(name).equals("jar");
    public final static FilenameFilter ACCEPT_ALL_FILTER = (dir, name) -> !name.equalsIgnoreCase("eula.txt");
    public final static FilenameFilter ACCEPT_NONE_FILTER = (dir, name) -> false;
    public final static FilenameFilter ACCEPT_FORGE_FILTER = (dir, name) -> (name.startsWith("forge") || name.startsWith("FTBserver")) && ACCEPT_ALL_JAR_FILTER.accept(dir, name);
    public final static FilenameFilter ACCEPT_MINECRAFT_SERVER_FILTER = (dir, name) -> name.startsWith("minecraft_server") && ACCEPT_ALL_JAR_FILTER.accept(dir, name);

    public static final File ROOT = getRootFile();
    public static final File CONFIG_FILE = new File(ROOT, "config.json");
    public static final File SERVERS_FILE = new File(ROOT, "servers.json");
    public static final File USERS_FILE = new File(ROOT, "users.json");
    public static final File FORGE_FILE = new File(ROOT, "forge.json");
    public static final File SERVERS = new File(ROOT, "servers");
    public static final File BACKUPS = new File(ROOT, "backups");

    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[0-9A-Za-z]+$");
    public static final Pattern SERVERNAME_PATTERN = Pattern.compile("^\\w+$");
    public static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)+");

    public static final String SERVER_START_ARGS_BLACKLIST_PATTERNS[] = {"-Xms", "-Xmx", "-XX:MaxPermSize"};
    public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();
    public static final JsonParser JSONPARSER = new JsonParser();
    public static final Joiner JOINER_COMMA_SPACE = Joiner.on(", ");
    public static final SimpleDateFormat BACKUP_SDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    public static final Random RANDOM = new Random();

    private static String javaPath;

    private Constants()
    {
    }

    private static File getRootFile()
    {
        try
        {
            return new File(".").getCanonicalFile();
        }
        catch (IOException e)
        {
            return new File(".");
        }
    }

    public static String getJavaPath()
    {
        if (javaPath == null)
        {
            JavaInfo javaVersion;
            if (OSUtils.getCurrentOS() == OSUtils.OS.MACOSX)
            {
                javaVersion = JavaFinder.parseJavaVersion();

                if (javaVersion != null && javaVersion.path != null) return javaPath = javaVersion.path;
            }
            else if (OSUtils.getCurrentOS() == OSUtils.OS.WINDOWS)
            {
                javaVersion = JavaFinder.parseJavaVersion();

                if (javaVersion != null && javaVersion.path != null) return javaPath = javaVersion.path.replace(".exe", "w.exe");
            }

            // Windows specific code adds <java.home>/bin/java no need mangle javaw.exe here.
            return javaPath = System.getProperty("java.home") + "/bin/java";
        }
        return javaPath;
    }
}
