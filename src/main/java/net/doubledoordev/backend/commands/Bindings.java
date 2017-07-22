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

package net.doubledoordev.backend.commands;

import com.sk89q.intake.CommandMapping;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.Module;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;
import com.sk89q.intake.parametric.binder.Binder;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Settings;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Dries007
 */
public class Bindings implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(CommandMapping.class).toProvider(new Provider<CommandMapping>() {
            @Override
            public boolean isProvided()
            {
                return false;
            }

            @Override
            public CommandMapping get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException
            {
                CommandMapping c = CommandHandler.INSTANCE.dispatcher.get(arguments.next());
                if (c == null) throw new ProvisionException("Not a valid command!");
                return c;
            }

            @Override
            public List<String> getSuggestions(String prefix)
            {
                return CommandHandler.INSTANCE.dispatcher.getPrimaryAliases().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
        });

        binder.bind(Server.class).toProvider(new Provider<Server>() {
            @Override
            public boolean isProvided()
            {
                return false;
            }

            @Override
            public Server get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException
            {
                return Settings.getServerByName(arguments.next());
            }

            @Override
            public List<String> getSuggestions(String prefix)
            {
                return Settings.SETTINGS.servers.keySet().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
        });

        binder.bind(Server[].class).toProvider(new Provider<Server[]>() {
            @Override
            public boolean isProvided()
            {
                return false;
            }

            @Override
            public Server[] get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException
            {
                String selector = arguments.next();
                if (Settings.getServerByName(selector) != null) return new Server[]{Settings.getServerByName(selector)};
                if (selector.equals("*")) return Settings.SETTINGS.servers.values().toArray(new Server[0]);

                Pattern pattern = Pattern.compile(selector);
                List<Server> servers = new ArrayList<>();
                for (Server server : Settings.SETTINGS.servers.values()) if (pattern.matcher(server.getID()).matches()) servers.add(server);
                return servers.toArray(new Server[servers.size()]);
            }

            @Override
            public List<String> getSuggestions(String prefix)
            {
                return Settings.SETTINGS.servers.keySet().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
        });

        binder.bind(User.class).toProvider(new Provider<User>() {
            @Override
            public boolean isProvided()
            {
                return false;
            }

            @Override
            public User get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException
            {
                return Settings.getUserByName(arguments.next());
            }

            @Override
            public List<String> getSuggestions(String prefix)
            {
                return Settings.SETTINGS.users.keySet().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
        });

        binder.bind(User[].class).toProvider(new Provider<User[]>() {
            @Override
            public boolean isProvided()
            {
                return false;
            }

            @Override
            public User[] get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException
            {
                String selector = arguments.next();
                if (Settings.getUserByName(selector) != null) return new User[]{Settings.getUserByName(selector)};
                if (selector.equals("*")) return Settings.SETTINGS.users.values().toArray(new User[0]);

                Pattern pattern = Pattern.compile(selector);
                List<User> users = new ArrayList<>();
                for (User server : Settings.SETTINGS.users.values()) if (pattern.matcher(server.getUsername()).matches()) users.add(server);
                return users.toArray(new User[users.size()]);
            }

            @Override
            public List<String> getSuggestions(String prefix)
            {
                return Settings.SETTINGS.users.keySet().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
        });
    }
}
