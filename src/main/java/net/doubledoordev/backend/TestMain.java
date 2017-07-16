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

package net.doubledoordev.backend;

import net.doubledoordev.backend.server.query.MCQuery;

import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TestMain
{
    public static void main(String[] args) throws Exception
    {
//        testMCQuery();
        testTaskStuff();
    }

    private static void testTaskStuff()
    {
        Timer timer = new Timer();

//        timer.schedule();

//        Calendar now = Calendar.getInstance();
        Date now = Date.from(Instant.now());
//        Instant now = Instant.now();


        timer.scheduleAtFixedRate(new Task(), 0, 1000 * 10); // 10 sec
    }

    private static class Task extends TimerTask
    {
        @Override
        public void run()
        {
            System.out.println("Task run()");

            Calendar now = Calendar.getInstance();
        }
    }

    private static void testMCQuery() throws IOException
    {
        System.out.println(new MCQuery("vps2.dries007.net", 25501).fullStat());
    }

    private static void sleep1sec()
    {
        synchronized (Thread.currentThread())
        {
            try
            {
                Thread.currentThread().wait(1000);
            }
            catch (InterruptedException ignored)
            {

            }
        }
    }
}
