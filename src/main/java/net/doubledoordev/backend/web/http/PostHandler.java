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

package net.doubledoordev.backend.web.http;

import freemarker.template.TemplateException;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.Group;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.FileManager;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.PasswordHash;
import net.doubledoordev.backend.util.Settings;
import net.doubledoordev.backend.util.exceptions.OutOfPortsException;
import net.doubledoordev.backend.util.exceptions.PostException;
import net.doubledoordev.backend.util.methodCaller.UserMethodCaller;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.http.multipart.MultipartScanner;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Parameters;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static net.doubledoordev.backend.util.Constants.*;

/**
 * @author Dries007
 */
public class PostHandler
{
    public static final PostHandler POST_HANDLER = new PostHandler();
    /*
     * FORM field names
     */
    private static final String OWNER = "owner";
    private static final String NAME = "name";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String OLD_PASSWORD = "oldPassword";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String ARE_YOU_HUMAN = "areyouhuman";
    private static final String RAM_MIN = "RAMmin";
    private static final String RAM_MAX = "RAMmax";
    private static final String PERMGEN = "PermGen";
    private static final String EXTRA_JAVA_PARM = "extraJavaParameters";
    private static final String EXTRA_MC_PARM = "extraMCParameters";
    private static final String ADMINS = "admins";
    private static final String COOWNERS = "coOwners";
    private static final String JARNAME = "jarname";
    private static final String RCON_PASS = "rconpass";
    private static final String RCON_PORT = "rconport";
    private static final String SERVER_PORT = "serverport";
    private static final String IP = "ip";
    private static final String AUTOSTART = "autostart";
    private static final String LOGOUT = "logout";

    private PostHandler()
    {
    }

    public String handle(HashMap<String, Object> data, String uri, Request request, Response response) throws Exception
    {
        try
        {
            switch (uri)
            {
                case LOGIN_URL:
                    return doLogin(uri, request, response);
                case REGISTER_URL:
                    return doRegister(uri, request, response);
                case NEWSERVER_URL:
                    return doNewserver(data, uri, request, response);
                case FILEMANAGER_URL:
                    return doFilemanager(data, uri, request, response);
            }
        }
        catch (RuntimeException e)
        {
            data.put(MESSAGE, e.getLocalizedMessage());
        }
        return uri;
    }

    private String doFilemanager(final HashMap<String, Object> data, final String uri, final Request request, final Response response) throws IOException
    {
        User user = (User) data.get(USER);

        if (user == null) throw new PostException("Not logged in.");
        final Server server = Settings.getServerByName(request.getParameter(SERVER));
        if (server == null || !server.canUserControl(user)) throw new PostException("Server doesn't exist or user doesn't have permission to edit the server.");
        data.put(SERVER, server);
        final FileManager fileManager = new FileManager(server, request.getParameter(FILE));
        data.put("fm", fileManager);

        response.suspend();

        final UploaderMultipartHandler uploader = new UploaderMultipartHandler(fileManager);
        MultipartScanner.scan(request, uploader, new EmptyCompletionHandler<Request>()
        {
            @Override
            public void completed(final Request newRequest)
            {
                Response response = newRequest.getResponse();
                if (response.getResponse() == null) return;
                try
                {
                    String uri_ = uri;
                    /**
                     * fix up the url to match template
                     */
                    if (uri_.endsWith(SLASH_STR)) uri_ += INDEX;
                    if (uri_.startsWith(SLASH_STR)) uri_ = uri_.substring(1);

                    if (!uri_.endsWith(Constants.TEMPLATE_EXTENSION)) uri_ += Constants.TEMPLATE_EXTENSION;

                    Main.getFreemarkerHandler().freemarker.getTemplate(uri_).process(data, response.getWriter());
                }
                catch (IOException | TemplateException ignored)
                {

                }

                response.resume();
            }

            @Override
            public void failed(Throwable throwable)
            {
                Main.LOGGER.warn("Upload failed to {}", fileManager.getFile().getAbsolutePath());
                throwable.printStackTrace();
                response.resume();
            }
        });

        return null;
    }

    private String doNewserver(HashMap<String, Object> data, String uri, Request request, Response response) throws IOException
    {
        User user = (User) data.get(USER);
        UserMethodCaller caller = new UserMethodCaller(user);

        Parameters parameters = request.getParameters();
        Set<String> names = request.getParameterNames();

        if (user == null) throw new PostException("Not logged in.");
        if (user.getMaxServers() != -1 && user.getServerCount() >= user.getMaxServers()) throw new PostException("Max server count reached.");

        String owner = user.getGroup() == Group.ADMIN && names.contains(OWNER) ? parameters.getParameter(OWNER) : user.getUsername();
        String ID = owner + "_" + parameters.getParameter(NAME);
        if (Settings.getServerByName(ID) != null) throw new PostException("Duplicate server ID");

        Server server = new Server(ID, owner);

        int ramMin = Integer.parseInt(parameters.getParameter(RAM_MIN));
        int ramMax = Integer.parseInt(parameters.getParameter(RAM_MAX));
        if (ramMax < ramMin)
        {
            int temp = ramMax;
            ramMax = ramMin;
            ramMin = temp;
        }
        if (user.getMaxRam() != -1 && user.getMaxRamLeft() < ramMax) throw new PostException("You are over your max RAM.");
        if (ramMax < 2 || ramMin < 2) throw new PostException("RAM settings invalid.");
        server.getJvmData().ramMin = ramMin;
        server.getJvmData().ramMax = ramMax;

        int permGen = Integer.parseInt(parameters.getParameter(PERMGEN));
        if (permGen < 2) throw new PostException("PermGen settings invalid.");
        server.getJvmData().permGen = permGen;

        if (parameters.getParameter(EXTRA_JAVA_PARM).trim().length() != 0) server.getJvmData().extraJavaParameters = parameters.getParameter(EXTRA_JAVA_PARM).trim();
        if (parameters.getParameter(EXTRA_MC_PARM).trim().length() != 0) server.getJvmData().extraMCParameters = parameters.getParameter(EXTRA_MC_PARM).trim();
        if (parameters.getParameter(ADMINS).trim().length() != 0) for (String name : Arrays.asList(parameters.getParameter(ADMINS).trim().split("\n"))) server.addAdmin(caller, name);
        if (parameters.getParameter(COOWNERS).trim().length() != 0) for (String name : Arrays.asList(parameters.getParameter(COOWNERS).trim().split("\n"))) server.addCoowner(caller, name);

        server.getJvmData().jarName = parameters.getParameter(JARNAME);
        try
        {
            server.setServerPort(caller, Settings.SETTINGS.fixedPorts ? Settings.SETTINGS.portRange.getNextAvailablePort() : Integer.parseInt(parameters.getParameter(SERVER_PORT)));
        }
        catch (OutOfPortsException e)
        {
            throw new PostException("The backend ran out of ports to assign.");
        }
        if (names.contains(IP)) server.setIP(caller, parameters.getParameter(IP));
        server.getRestartingInfo().autoStart = names.contains(AUTOSTART) && parameters.getParameter(AUTOSTART).equals("on");

        server.init();

        Settings.SETTINGS.servers.put(ID, server);
        Settings.save();
        data.put(SERVER, server);

        try
        {
            response.sendRedirect(Constants.SERVER_URL + ID);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return uri;
    }

    private String doRegister(String uri, Request request, Response response)
    {
        Parameters parameters = request.getParameters();
        Set<String> names = request.getParameterNames();

        if (names.contains(USERNAME) && names.contains(PASSWORD) && names.contains(ARE_YOU_HUMAN))
        {
            String username = parameters.getParameter(USERNAME);
            boolean admin = Main.adminKey != null && parameters.getParameter(ARE_YOU_HUMAN).equals(Main.adminKey);
            if (!admin && !parameters.getParameter(ARE_YOU_HUMAN).trim().equals("4")) throw new PostException("You failed the human test...");
            User user = Settings.getUserByName(username);
            if (user != null) throw new PostException("Username taken.");
            if (!USERNAME_PATTERN.matcher(username).matches()) throw new PostException("Username contains invalid chars.<br>Only a-Z, 0-9, _ and - please.");
            try
            {
                user = new User(username, PasswordHash.createHash(parameters.getParameter(PASSWORD)));
                if (admin)
                {
                    user.setGroup(Group.ADMIN);
                    Main.adminKey = null;
                    Main.LOGGER.warn("Admin key claimed. You cannot use it anymore!");
                    user.setMaxRam(-1);
                    user.setMaxDiskspace(-1);
                    user.setMaxServers(-1);
                }
                Settings.SETTINGS.users.put(user.getUsername().toLowerCase(), user);
                request.getSession().setAttribute(USER, user);
                Settings.save();

                return LOGIN_URL;
            }
            catch (NoSuchAlgorithmException | InvalidKeySpecException e)
            {
                // Hash algorithm doesn't work.
                throw new RuntimeException(e);
            }
        }
        else throw new PostException("Form not of known format.");
    }

    private String doLogin(String uri, Request request, Response response)
    {
        Parameters parameters = request.getParameters();
        Set<String> names = request.getParameterNames();

        if (names.contains(USERNAME) && names.contains(PASSWORD))
        {
            User user = Settings.getUserByName(parameters.getParameter(USERNAME));
            if (user == null) throw new PostException(String.format("User %s can't be found.", parameters.getParameter(USERNAME)));
            if (!user.verify(parameters.getParameter(PASSWORD))) throw new PostException("Password wrong.");
            request.getSession().setAttribute(USER, user);
        }
        else if (names.contains(LOGOUT))
        {
            request.getSession().attributes().clear();
            request.changeSessionId();
        }
        else if (names.contains(OLD_PASSWORD) && names.contains(NEW_PASSWORD))
        {
            User user = (User) request.getSession().getAttribute(USER);
            if (!user.updatePassword(parameters.getParameter(OLD_PASSWORD), parameters.getParameter(NEW_PASSWORD))) throw new PostException("Password wrong.");
        }
        else throw new PostException("Form not of known format.");

        return uri;
    }
}
