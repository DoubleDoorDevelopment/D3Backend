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

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.util.IUpdateFromJson;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoField.*;

/**
 * The auto-restart doesn't need to be on a repeated timer, because the shutdown and boot will stop and restart the timer.
 *
 * @author Dries007
 */
public class RestartingInfo implements IUpdateFromJson
{
    private static final int[] COUNTDOWN = new int[] {15, 10, 5, 4, 3, 2, 1};

    @Expose
    public boolean autoStart = false;
    @Expose
    public boolean enableRestartSchedule = false;
    @Expose
    public int restartScheduleHours = 0;
    @Expose
    public int restartScheduleMinutes = 0;
    @Expose
    public String restartScheduleMessage = "Server reboot in %time minutes!";

    @Override
    public void updateFrom(JsonObject json)
    {
        if (json.has("autoStart")) autoStart = json.get("autoStart").getAsBoolean();

        if (json.has("enableRestartSchedule")) enableRestartSchedule = json.get("enableRestartSchedule").getAsBoolean();
        if (json.has("restartScheduleHours")) restartScheduleHours = json.get("restartScheduleHours").getAsInt();
        if (json.has("restartScheduleMinutes")) restartScheduleMinutes = json.get("restartScheduleMinutes").getAsInt();

        if (json.has("enableRestartSchedule") || json.has("restartScheduleHours") || json.has("restartScheduleMinutes"))
        {
            start();
        }

        if (json.has("restartScheduleMessage")) restartScheduleMessage = json.get("restartScheduleMessage").getAsString();
    }

    private Server server;

    public void init(Server server)
    {
        this.server = server;
    }

    private Timer timer;
    private Date lastRestart;
    private Date nextRestart;

    public void start()
    {
        if (timer != null) stop();
        if (!enableRestartSchedule) return;
        timer = new Timer();

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime restartTime = now.with(HOUR_OF_DAY, restartScheduleHours).with(MINUTE_OF_HOUR, restartScheduleMinutes).with(SECOND_OF_MINUTE, 0).with(MICRO_OF_SECOND, 0);
        if (restartTime.minusMinutes(1).isBefore(now)) restartTime = restartTime.plusDays(1);

        nextRestart = Date.from(restartTime.toInstant());

        for (int minuteOffset : COUNTDOWN)
        {
            ZonedDateTime warningTime = restartTime.minusMinutes(minuteOffset);
            if (warningTime.isBefore(now)) continue;
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    server.sendChat(restartScheduleMessage.replace("%time", Integer.toString(minuteOffset)));
                }
            }, Date.from(warningTime.toInstant()));
        }

        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                lastRestart = new Date();
                server.printLine("[AutoRestart] Sending stop command...");
                server.stopServer("[AutoRestart] Restarting on schedule.");
                waitForShutdown(150);
                if (server.getOnline())
                {
                    server.printLine("[AutoRestart] Force-stopping server...");
                    try
                    {
                        server.forceStopServer();
                        waitForShutdown(30);
                    }
                    catch (Exception ignored)
                    {

                    }
                    if (server.getOnline())
                    {
                        server.printLine("[AutoRestart] Murdering server...");
                        try
                        {
                            server.murderServer();
                            waitForShutdown(30);
                        }
                        catch (Exception ignored)
                        {

                        }
                        if (server.getOnline())
                        {
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
                    server.startServer();
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

        }, Date.from(restartTime.toInstant()));
    }

    public void stop()
    {
        if (timer == null) return;

        timer.cancel();
        timer.purge();
        timer = null;
        nextRestart = null;
    }

    public String getLastRestart(String format)
    {
        return lastRestart == null ? "None." : new SimpleDateFormat(format).format(lastRestart);
    }

    public String getNextRestart(String format)
    {
        return nextRestart == null ? "None." : new SimpleDateFormat(format).format(nextRestart);
    }
}
