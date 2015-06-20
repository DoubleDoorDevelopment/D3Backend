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

import com.google.common.collect.ImmutableList;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.*;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.FileManager;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.Helper;
import net.doubledoordev.backend.util.Settings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static net.doubledoordev.backend.util.Constants.*;
import static net.doubledoordev.backend.util.Settings.SETTINGS;
import static net.doubledoordev.backend.web.http.PostHandler.POST_HANDLER;

/**
 * Freemarker!
 *
 * @author Dries007
 */
public class FreemarkerHandler extends StaticHttpHandlerBase implements ErrorPageGenerator
{
    public static final ImmutableList<String> ADMINPAGES = ImmutableList.of("console", "backendConsoleText");
    public static long lastRequest = 0L;
    public final Configuration freemarker = new Configuration();

    public FreemarkerHandler(Class clazz, String path) throws TemplateModelException
    {
        freemarker.setClassForTemplateLoading(clazz, path);
        freemarker.setObjectWrapper(new DefaultObjectWrapper());
        freemarker.setDefaultEncoding("UTF-8");
        freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        freemarker.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20
        Map<String, Object> dataObject = new HashMap<>();
        dataObject.put("Settings", Settings.SETTINGS);
        dataObject.put("Helper", BeansWrapper.getDefaultInstance().getStaticModels().get(Helper.class.getName()));
        freemarker.setAllSharedVariables(new SimpleHash(dataObject));

        if (!Main.debug) freemarker.setTemplateUpdateDelay(0);
    }

    @Override
    protected boolean handle(String uri, Request request, Response response) throws Exception
    {
        if (request.getServerPort() == Settings.SETTINGS.portHTTP && Strings.isNotBlank(SETTINGS.certificatePath))
        {
            StringBuilder redirect = new StringBuilder();
            redirect.append("https://").append(request.getServerName());
            if (SETTINGS.portHTTPS != 443) redirect.append(':').append(SETTINGS.portHTTPS);
            redirect.append(request.getRequest().getRequestURI());
            if (request.getRequest().getQueryString() != null) redirect.append('?').append(request.getRequest().getQueryString());
            response.sendRedirect(redirect.toString());
            return true;
        }

        lastRequest = System.currentTimeMillis();
        if (request.getSession(false) != null) request.getSession();

        HashMap<String, Object> data = new HashMap<>(request.getSession().attributes().size() + 10);
        // Put all session data in map, take 1
        data.putAll(request.getSession().attributes());

        /**
         * Data processing
         */
        if (request.getMethod() == Method.GET)
        {
            Server server = Settings.getServerByName(request.getParameter(SERVER));
            if (server != null && server.canUserControl((User) data.get(USER)))
            {
                data.put(SERVER, server);
                if (uri.equals(FILEMANAGER_URL)) data.put("fm", new FileManager(server, request.getParameter(FILE)));
            }
        }
        else if (request.getMethod() == Method.POST)
        {
            uri = POST_HANDLER.handle(data, uri, request, response);
        }
        else
        {
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED_405.getStatusCode());
        }

        if (uri == null) return true;

        /**
         * fix up the url to match template
         */
        if (uri.endsWith(SLASH_STR)) uri += INDEX;
        if (uri.startsWith(SLASH_STR)) uri = uri.substring(1);

        if (request.getSession().getAttribute(USER) == null && !Settings.SETTINGS.anonPages.contains(uri))
        {
            response.sendError(HttpStatus.UNAUTHORIZED_401.getStatusCode());
            return true;
        }
        else if (ADMINPAGES.contains(uri) && !((User) request.getSession().getAttribute(USER)).isAdmin())
        {
            response.sendError(HttpStatus.UNAUTHORIZED_401.getStatusCode());
            return true;
        }
        if (!uri.endsWith(Constants.TEMPLATE_EXTENSION)) uri += Constants.TEMPLATE_EXTENSION;

        // Put all session data in map, take 2
        data.putAll(request.getSession().attributes());

        try
        {
            freemarker.getTemplate(uri).process(data, response.getWriter());
            return true;
        }
        catch (FileNotFoundException ignored)
        {
            return false;
        }
    }

    @Override
    public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception)
    {
        HashMap<String, Object> data = new HashMap<>(request.getSession().attributes().size() + 10);
        data.putAll(request.getSession().attributes());
        data.put(STATUS, status);
        data.put("reasonPhrase", reasonPhrase);
        data.put("description", description);
        data.put("exception", exception);
        if (exception != null) data.put("stackTrace", ExceptionUtils.getStackTrace(exception));

        StringWriter stringWriter = new StringWriter();
        try
        {
            freemarker.getTemplate(ERROR_TEMPLATE).process(data, stringWriter);
        }
        catch (Exception e)
        {
            e.printStackTrace(new PrintWriter(stringWriter));
        }
        return stringWriter.toString();
    }
}
