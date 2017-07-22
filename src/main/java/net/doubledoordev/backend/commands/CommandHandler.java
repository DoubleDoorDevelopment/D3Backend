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

import com.google.common.collect.ImmutableList;
import com.sk89q.intake.*;
import com.sk89q.intake.argument.Namespace;
import com.sk89q.intake.dispatcher.Dispatcher;
import com.sk89q.intake.dispatcher.SimpleDispatcher;
import com.sk89q.intake.parametric.Injector;
import com.sk89q.intake.parametric.ParametricBuilder;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Switch;
import com.sk89q.intake.parametric.annotation.Text;
import com.sk89q.intake.parametric.provider.DefaultModule;
import com.sk89q.intake.parametric.provider.PrimitivesModule;
import com.sk89q.intake.util.auth.AuthorizationException;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.permissions.Group;
import net.doubledoordev.backend.permissions.User;
import net.doubledoordev.backend.server.Server;
import net.doubledoordev.backend.util.Cache;
import net.doubledoordev.backend.util.Constants;
import net.doubledoordev.backend.util.methodCaller.IMethodCaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;

import static net.doubledoordev.backend.util.Constants.JOINER_COMMA_SPACE;
import static net.doubledoordev.backend.util.Settings.SETTINGS;

/**
 * Using sk89q's Intake lib
 *
 * @author Dries007
 */
public class CommandHandler implements Runnable
{
    public static final CommandHandler INSTANCE = new CommandHandler();
    public static final Logger CMDLOGGER = LogManager.getLogger("cmd");
    public static final User CMDUSER = new User(null, null)
    {
        //@formatter:off
        @Override public boolean verify(String password) { return false; }
        @Override public boolean updatePassword(String oldPass, String newPass) { return false; }
        @Override public void setPass(String newPass) {}
        @Override public String getUsername() { return "[" + Constants.NAME + "]"; }
        @Override public Group getGroup() { return Group.ADMIN; }
        @Override public void setGroup(Group group) {}
        @Override public void setGroup(String group) {}
        @Override public int getMaxServers() { return -1; }
        @Override public void setMaxServers(int maxServers) {}
        @Override public int getMaxRam() { return -1; }
        @Override public void setMaxRam(int maxRam) {}
        @Override public int getMaxRamLeft() { return -1; }
        @Override public int getServerCount() { return 0; }
        @Override public int getMaxDiskspace() { return -1; }
        @Override public void setMaxDiskspace(int maxDiskspace) {}
        @Override public int getDiskspaceLeft() { return -1; }
        @Override public boolean isAdmin() { return true; }
        @Override public void delete() {}
        //@formatter:on
    };
    public static final IMethodCaller CMDCALLER = new IMethodCaller()
    {
        @Override
        public User getUser()
        {
            return CMDUSER;
        }

        //@formatter:off
        @Override public void sendOK() {}
        @Override public void sendMessage(String message) {}
        //@Override public void sendProgress(float progress) {}
        @Override public void sendError(String message) {}
        @Override public void sendDone() {}
        //@formatter:on
    };
    public final Dispatcher dispatcher;

    private CommandHandler()
    {
        Injector i = Intake.createInjector();
        i.install(new Bindings());
        i.install(new DefaultModule());
        i.install(new PrimitivesModule());
        ParametricBuilder parametricBuilder = new ParametricBuilder(i);
        parametricBuilder.setCommandExecutor(Executors.newSingleThreadExecutor());
        dispatcher = new SimpleDispatcher();
        parametricBuilder.registerMethodsAsCommands(dispatcher, this);
    }

    public static void init()
    {
        new Thread(INSTANCE, "CommandHandler").start();
    }

    @Override
    public void run()
    {
        Console console = System.console();
        if (console != null)
        {
            while (Main.running)
            {
                command(console.readLine());
            }
            return;
        }
        if (System.in != null && Main.debug) // Only allow when debug is on
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (Main.running)
            {
                try
                {
                    command(in.readLine());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            return;
        }

        JOptionPane.showMessageDialog(null, "You opened D3Backend without a console. Use a commandline environment to open the jar for now.");
        System.exit(1);
    }

    private void command(String command)
    {
        try
        {

            if (dispatcher.get(command.split(" ")[0]) != null) dispatcher.call(command, new Namespace(), ImmutableList.of());
            else throw new CommandNotFoundException(command);
        }
        catch (CommandException | InvocationCommandException | AuthorizationException e)
        {
            CMDLOGGER.warn(e);
            e.printStackTrace();
        }
    }

    @Command(aliases = {"help", "?"}, desc = "Get a list of commands", help = "Use this to get help", usage = "[Command]", max = 1)
    public void cmdHelp(@Optional CommandMapping command) throws CommandException
    {
        // Command list
        if (command == null)
        {
            CMDLOGGER.info("--==## Command list ##==--");
            for (CommandMapping cmd : dispatcher.getCommands())
            {
                CMDLOGGER.info(cmd.getPrimaryAlias() + ' ' + cmd.getDescription().getUsage() + " => " + cmd.getDescription().getShortDescription()); // Looks like this: Name ListOfParameters => Description
            }
        }
        else
        {
            CMDLOGGER.info(String.format("--==## Help for %s ##==--", command.getPrimaryAlias()));
            CMDLOGGER.info(String.format("Name: %s \t Aliases: %s", command.getPrimaryAlias(), JOINER_COMMA_SPACE.join(command.getAllAliases())));
            CMDLOGGER.info(String.format("Usage: %s %s", command.getPrimaryAlias(), command.getDescription().getUsage()));
            CMDLOGGER.info(String.format("Short description: %s", command.getDescription().getShortDescription()));
            CMDLOGGER.info(String.format("Help text: %s", command.getDescription().getHelp()));
        }
    }

    @Command(aliases = {"serverlist", "servers"}, desc = "List all servers", max = 0)
    public void cmdServerList()
    {
        CMDLOGGER.info("All servers:");
        CMDLOGGER.info(JOINER_COMMA_SPACE.join(SETTINGS.getServers()));
        CMDLOGGER.info("Online servers:");
        CMDLOGGER.info(JOINER_COMMA_SPACE.join(SETTINGS.getOnlineServers()));
    }

    @Command(aliases = "message", desc = "Send message to servers (with /say)", usage = "<server ID (regex)> <message ...>", min = 2)
    public void cmdMessage(Server[] servers, @Text String msg) throws CommandException
    {
        for (Server server : servers)
        {
            if (!server.getOnline()) continue;
            server.sendChat(CMDCALLER, msg);
        }
    }

    @Command(aliases = "backup", desc = "Make full backup of one or more servers", usage = "<server ID (regex)>", min = 1, max = 1)
    public void cmdBackup(Server[] servers) throws CommandException
    {
        for (Server server : servers)
        {
            server.getWorldManager().doMakeAllOfTheBackup(CMDCALLER);
        }
    }

    @Command(aliases = "stop", desc = "Stop one or more servers", usage = "<server ID (regex)> [-f (force the stop)] [message ...]", min = 1)
    public void cmdStop(Server[] servers, @Switch('f') boolean force, @Optional("Stopping the server.") @Text String msg) throws CommandException
    {
        for (Server server : servers)
        {
            if (!server.getOnline()) continue;
            if (server.stopServer(CMDCALLER, msg)) CMDLOGGER.info(String.format("Shutdown command send to %s", server.getID()));
            else CMDLOGGER.warn(String.format("Server %s did not shutdown with a message.", server.getID()));
        }
    }

    @Command(aliases = "start", desc = "Start one or more servers", usage = "<server ID (regex)>", min = 1)
    public void cmdStart(Server[] servers, @Switch('f') boolean force, @Optional("Stopping the server.") @Text String msg) throws CommandException
    {
        for (Server server : servers)
        {
            if (server.getOnline()) continue;
            try
            {
                server.startServer(CMDCALLER);
            }
            catch (Exception e)
            {
                CMDLOGGER.warn("Not able to start server " + server.getID());
                CMDLOGGER.warn(e);
            }
        }
    }

    @Command(aliases = "version", desc = "Get current version and latest version available.", usage = "", min = 0, max = 0)
    public void cmdVersion() throws CommandException
    {
        CMDLOGGER.info("Current version: {}. Build: {}", Main.version, Main.build);
        CMDLOGGER.info("Latest version available: {}", Cache.getUpdateVersion());
    }

    @Command(aliases = {"shutdown", "exit"}, usage = "", desc = "Stop the backend", max = 0, flags = "f")
    public void cmdShutdown(@Switch('f') boolean force) throws CommandException
    {
        if (force) System.exit(0);
        Main.shutdown();
    }

    @Command(aliases = {"command", "cmd"}, desc = "Send a command to one or more servers", usage = "<server ID (regex)> <message ...>", min = 2)
    public void cmdCommand(Server[] servers, @Text String cmd) throws CommandException
    {
        for (Server server : servers)
        {
            if (!server.getOnline()) continue;
            server.sendCmd(CMDCALLER, cmd);
        }
    }

    @Command(aliases = "update", desc = "Force an update on Forge or MC versions", usage = "<forge|mc>", min = 1)
    public void updateCommand(String subcmd) throws CommandException
    {
        if (subcmd.equalsIgnoreCase("forge"))
        {
            Cache.forceUpdateForge();
            CMDLOGGER.info("Force updating forge");
        }
        else if (subcmd.equalsIgnoreCase("mc"))
        {
            Cache.forceUpdateMC();
            CMDLOGGER.info("Force updating MC versions");
        }
        else CMDLOGGER.info("Subcommand '{}' unknown.", subcmd);
    }

    @Command(aliases = "license", desc = "Show this product's license information")
    public void license()
    {
        CMDLOGGER.info(
                "    D3Backend\n" +
                        "    Copyright (C) 2015 - 2016 Dries007 & Double Door Development\n" +
                        "\n" +
                        "    This program is free software: you can redistribute it and/or modify\n" +
                        "    it under the terms of the GNU Affero General Public License as published\n" +
                        "    by the Free Software Foundation, either version 3 of the License, or\n" +
                        "    (at your option) any later version.\n" +
                        "\n" +
                        "    This program is distributed in the hope that it will be useful,\n" +
                        "    but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                        "    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
                        "    GNU Affero General Public License for more details.\n" +
                        "\n" +
                        "    You should have received a copy of the GNU Affero General Public License\n" +
                        "    along with this program.  If not, see <http://www.gnu.org/licenses/>.");
    }
}
