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

package net.doubledoordev.backend.web.http;

import com.google.common.base.Strings;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Settings;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.File;

import static net.doubledoordev.backend.util.Constants.SLASH_STR;
import static net.doubledoordev.backend.util.Constants.USER;

/**
 * @author Dries007
 */
public class ServerFileHandler extends StaticHttpHandlerBase
{
    private final String path;

    public ServerFileHandler(String path)
    {
        this.path = path;
    }

    public ServerFileHandler()
    {
        this.path = null;
    }

    @Override
    protected boolean handle(String uri, Request request, Response response) throws Exception
    {
        if (request.getSession(false) != null) request.getSession();
        if (uri.startsWith(SLASH_STR)) uri = uri.substring(1);

        String[] uris = uri.split("/", 2); // 0 = server, 1 = file
        if (uris.length != 2) return false;

        Server server = Settings.getServerByName(uris[0]);
        if (server == null) return false;

        User user = (User) request.getSession().getAttribute(USER);
        if (user == null || !(user.isAdmin() || server.canUserControl(user)))
        {
            response.setHeader(Header.Allow, Method.GET.getMethodString());
            response.sendError(HttpStatus.UNAUTHORIZED_401.getStatusCode());
            return true;
        }

        File baseFolder = Strings.isNullOrEmpty(path) ? server.getFolder() : new File(server.getFolder(), path);

        File file = new File(baseFolder, uris[1]);
        if (!file.exists() || file.isDirectory()) return false;

        if (!Method.GET.equals(request.getMethod()))
        {
            response.setHeader(Header.Allow, Method.GET.getMethodString());
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED_405.getStatusCode());
            return true;
        }

        pickupContentType(response, file.getPath());

        addToFileCache(request, response, file);
        sendFile(response, file);

        return true;
    }
}
