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

package net.doubledoordev.backend.commands;

import com.sk89q.intake.parametric.ParameterException;
import com.sk89q.intake.parametric.argument.ArgumentStack;
import com.sk89q.intake.parametric.binding.BindingBehavior;
import com.sk89q.intake.parametric.binding.BindingHelper;
import com.sk89q.intake.parametric.binding.BindingMatch;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Dries007
 */
public class Bindings extends BindingHelper
{
    @BindingMatch(type = Server.class, behavior = BindingBehavior.CONSUMES, consumedCount = 1)
    public Server getServer(ArgumentStack context) throws ParameterException
    {
        return Settings.getServerByName(context.next());
    }

    @BindingMatch(type = Server[].class, behavior = BindingBehavior.CONSUMES)
    public Server[] getServers(ArgumentStack context) throws ParameterException
    {
        Pattern pattern = Pattern.compile(context.next());
        List<Server> servers = new ArrayList<>();
        for (Server server : Settings.SETTINGS.servers.values()) if (pattern.matcher(server.getID()).matches()) servers.add(server);
        return servers.toArray(new Server[servers.size()]);
    }

    @BindingMatch(type = User.class, behavior = BindingBehavior.CONSUMES, consumedCount = 1)
    public User getUser(ArgumentStack context) throws ParameterException
    {
        return Settings.getUserByName(context.next());
    }

    @BindingMatch(type = User[].class, behavior = BindingBehavior.CONSUMES, consumedCount = 1)
    public User[] getUsers(ArgumentStack context) throws ParameterException
    {
        Pattern pattern = Pattern.compile(context.next());
        List<User> users = new ArrayList<>();
        for (User server : Settings.SETTINGS.users.values()) if (pattern.matcher(server.getUsername()).matches()) users.add(server);
        return users.toArray(new User[users.size()]);
    }
}
