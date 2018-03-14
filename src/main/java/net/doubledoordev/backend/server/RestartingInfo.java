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

package net.doubledoordev.backend.server;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.util.IUpdateFromJson;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimerTask;

import static java.time.temporal.ChronoField.*;
import static net.doubledoordev.backend.commands.CommandHandler.CMDCALLER;

/**
 * The auto-restart doesn't need to be on a repeated timer, because the shutdown and boot will stop and restart the timer.
 *
 * @author Dries007
 */
public class RestartingInfo implements IUpdateFromJson
{
    public static final CronParser CRON_PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J));

    @Expose
    public boolean autoStart = false;
    @Expose
    public boolean enableRestartSchedule = false;
    @Expose
    public String cronString = "";
    @Expose
    public String restartScheduleMessage = "Server reboot in %time seconds!";

    @Override
    public void updateFrom(JsonObject json)
    {
        if (json.has("autoStart")) autoStart = json.get("autoStart").getAsBoolean();

        if (json.has("enableRestartSchedule")) enableRestartSchedule = json.get("enableRestartSchedule").getAsBoolean();
        if (json.has("cronString"))
        {
            if (!cronString.equals(json.get("cronString").getAsString()))
            {
                cronString = json.get("cronString").getAsString();
                start();
            }
        }
        if (json.has("restartScheduleMessage")) restartScheduleMessage = json.get("restartScheduleMessage").getAsString();
    }

    private Server server;
    private Scheduler scheduler;
    private Date lastRestart;

    public void init(Server server)
    {
        this.server = server;
    }

    public void start()
    {
        if (scheduler != null) scheduler.stop();

        if (!enableRestartSchedule || Strings.isNullOrEmpty(cronString)) return;

        try
        {
            CRON_PARSER.parse(cronString).validate();
        }
        catch (Exception e)
        {
            server.error(e);
            return;
        }

        scheduler = new Scheduler();
        scheduler.start();

        server.printLine("[AutoRestart] Starting restart schedule: " + getHumanCronString());
        scheduler.schedule(cronString, new Task()
        {
            @Override
            public boolean canBeStopped()
            {
                return true;
            }

            public boolean sendMessage(int timeleft, int delay)
            {
                server.sendChat(CMDCALLER, restartScheduleMessage.replace("%time", Integer.toString(timeleft)));
                try
                {
                    Thread.sleep(delay*1000);
                }
                catch (InterruptedException ignored)
                {
                    return false;
                }
                return true;
            }

            @Override
            public void execute(TaskExecutionContext context) throws RuntimeException
            {
                server.printLine("[AutoRestart] Starting restart messages with 60 seconds on the clock.");

                sendMessage(60, 30); if (context.isStopped()) return;
                sendMessage(30, 15); if (context.isStopped()) return;
                sendMessage(15, 5);  if (context.isStopped()) return;
                sendMessage(10, 5);  if (context.isStopped()) return;
                sendMessage(5, 1);   if (context.isStopped()) return;
                sendMessage(4, 1);   if (context.isStopped()) return;
                sendMessage(3, 1);   if (context.isStopped()) return;
                sendMessage(2, 1);   if (context.isStopped()) return;
                sendMessage(1, 1);   if (context.isStopped()) return;

                lastRestart = new Date();
                server.printLine("[AutoRestart] Sending stop command...");
                server.stopServer(CMDCALLER, "[AutoRestart] Restarting on schedule.");
                waitForShutdown(150);
                if (server.getOnline())
                {
                    if (context.isStopped()) return;
                    server.printLine("[AutoRestart] Force-stopping server...");
                    try
                    {
                        server.forceStopServer(CMDCALLER);
                        waitForShutdown(30);
                    }
                    catch (Exception ignored)
                    {

                    }
                    if (server.getOnline())
                    {
                        if (context.isStopped()) return;
                        server.printLine("[AutoRestart] Murdering server...");
                        try
                        {
                            server.murderServer(CMDCALLER);
                            waitForShutdown(30);
                        }
                        catch (Exception ignored)
                        {

                        }
                        if (server.getOnline())
                        {
                            if (context.isStopped()) return;
                            server.printLine("[AutoRestart] Failed to make server stop. Can't restart it.");
                            Main.LOGGER.error("[AutoRestart] Failed to restart {}. It wouldn't stop.", server);
                            return;
                        }
                    }
                }

                try
                {
                    server.printLine("[AutoRestart] Restarting the server...");
                    Thread.sleep(1000);
                    server.startServer(CMDCALLER);
                }
                catch (Exception e)
                {
                    server.printLine("[AutoRestart] Failed to start the server. " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            private void waitForShutdown(int maxDelay)
            {
                while (server.getOnline() && maxDelay > 0)
                {
                    server.printLine("[AutoRestart] Waiting for shutdown. (" + maxDelay + "s of patience left.)");
                    maxDelay--;
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ignored)
                    {

                    }
                }
            }
        });
    }

    public void stop()
    {
        if (scheduler == null || !scheduler.isStarted()) return;
        scheduler.stop();
        scheduler = null;
    }

    public String getLastRestart(String format)
    {
        return lastRestart == null ? "None." : new SimpleDateFormat(format).format(lastRestart);
    }

    public String getHumanCronString()
    {
        if (Strings.isNullOrEmpty(cronString)) return "No schedule provided.";
        try
        {
            Cron cron = CRON_PARSER.parse(cronString).validate();
            return CronDescriptor.instance().describe(cron);
        }
        catch (Exception e)
        {
            return "Cron string could not be parsed.";
        }
    }
}
