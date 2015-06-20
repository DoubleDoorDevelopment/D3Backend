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

package net.doubledoordev.backend.server;

import net.doubledoordev.backend.util.Constants;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.util.Date;
import java.util.TimerTask;

/**
 * todo: reimplement
 * @author Dries007
 */
public class BackupTask extends TimerTask
{
    private final Server server;

    public BackupTask(Server server)
    {
        this.server = server;
    }

    @Override
    public void run()
    {
        if (server.getOnline())
        {
            server.sendChat("Making backup....");
            server.sendCmd("save-off");
            server.sendCmd("save-all");

            try
            {
                ZipParameters parameters = new ZipParameters();
                parameters.setIncludeRootFolder(false);
                ZipFile zipFile = new ZipFile(new File(server.getBackupFolder(), Constants.BACKUP_SDF.format(new Date()) + ".zip"));
                zipFile.addFolder(server.getFolder(), parameters);
            }
            catch (ZipException e)
            {
                server.sendChat("Error when making backup");
                server.printLine(e.toString());
            }

            server.sendCmd("save-on");
        }
    }

    private void removeOldestBackup()
    {
        File oldest = null;
        //noinspection ConstantConditions
        for (File file : server.getBackupFolder().listFiles())
        {
            if (oldest == null) oldest = file;
            if (file.lastModified() < oldest.lastModified()) oldest = file;
        }
        if (oldest == null || !oldest.delete()) server.printLine("Could not delete old backup file " + oldest);
        else server.printLine("Deleted old backup " + oldest);
    }
}
