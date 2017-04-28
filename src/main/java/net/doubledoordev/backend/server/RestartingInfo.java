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

import com.google.gson.annotations.Expose;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Dries007
 */
public class RestartingInfo
{
    @Expose
    public boolean autoStart = false;
//    @Expose
//    public int globalTimeout = 24;
//    @Expose
//    public int whenEmptyTimeout = -1;
    @Expose
    public boolean enableRestartSchedule = false;
    @Expose
    public int restartScheduleHours = 0;
    @Expose
    public int restartScheduleMinutes = 0;
    @Expose
    public String restartScheduleMessage = "Server reboot in %time minutes!";

    private boolean restartNextRun = false;
    private ScheduleStep runningSchedule = ScheduleStep.NONE;
    private Date lastRestart;

    public void run(Server server)
    {
        // To restart the server after it has been stopped by us.
        try
        {
            if (!server.getOnline() && !server.isDownloading() && restartNextRun)
            {
                server.startServer();
                restartNextRun = false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (!server.getOnline()) return;
//        if (lastRestart != null && System.currentTimeMillis() - lastRestart.getTime() < globalTimeout * 3600000) return;

        // Restart Schedule
        if (enableRestartSchedule)
        {
            Calendar calendar = Calendar.getInstance();
            switch (runningSchedule)
            {
                case NONE:
                    calendar.add(Calendar.MINUTE, 15);
                    if (calendar.get(Calendar.HOUR_OF_DAY) == restartScheduleHours && calendar.get(Calendar.MINUTE) == restartScheduleMinutes)
                    {
                        runningSchedule = ScheduleStep.M15;
                    }
                    break;
                default:
                    calendar.add(Calendar.MINUTE, runningSchedule.timeLeft);
                    if (calendar.get(Calendar.HOUR_OF_DAY) == restartScheduleHours && calendar.get(Calendar.MINUTE) == restartScheduleMinutes)
                    {
                        server.sendChat(restartScheduleMessage.replace("%time", Integer.toString(runningSchedule.timeLeft)));
                        runningSchedule = runningSchedule.nextStep;
                    }
                    break;
                case NOW:
                    runningSchedule = ScheduleStep.NONE;
                    initReboot(server, "Restarting on schedule.");
            }
        }

        // Empty check
//        if (server.getPlayerList().size() == 0 && whenEmptyTimeout != -1)
//        {
//            if (emptyDate == null) emptyDate = new Date();
//            else if (System.currentTimeMillis() - emptyDate.getTime() > whenEmptyTimeout * 60000)
//            {
//                initReboot(server, "Server restart because empty.");
//            }
//        }
//        else emptyDate = null;
    }

    private void initReboot(Server server, String s)
    {
        lastRestart = new Date();
        server.stopServer(s);
        restartNextRun = true;
    }

    public String getLastRestart(String format)
    {
        return lastRestart == null ? "" : new SimpleDateFormat(format).format(lastRestart);
    }

    public enum ScheduleStep
    {
        NONE(-1, null), NOW(0, NONE), M1(1, NOW), M2(2, M1), M3(3, M2), M4(4, M3), M5(5, M4), M10(10, M5), M15(15, M10);

        public final int timeLeft;
        public final ScheduleStep nextStep;

        ScheduleStep(int timeLeft, ScheduleStep nextStep)
        {
            this.timeLeft = timeLeft;
            this.nextStep = nextStep != null ? nextStep : this;
        }
    }
}
