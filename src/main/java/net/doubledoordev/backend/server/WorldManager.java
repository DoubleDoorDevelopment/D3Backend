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

import net.doubledoordev.backend.util.exceptions.BackupException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

import static net.doubledoordev.backend.Main.LOGGER;
import static net.doubledoordev.backend.util.Constants.*;

/**
 * Handles backups and dimension related stuff
 *
 * @author Dries007
 */
@SuppressWarnings("UnusedDeclaration")
public class WorldManager
{
    final Server server;
    public boolean bypassLimits = false;
    File worldFolder;

    public WorldManager(Server server)
    {
        this.server = server;
    }

    public void update()
    {
        worldFolder = new File(server.getFolder(), getWorldName());
        if (!worldFolder.exists()) return;
        if (worldFolder.list().length == 0) return;
        if (!server.getDimensionMap().containsKey(OVERWORLD)) server.getDimensionMap().put(OVERWORLD, new Dimension(OVERWORLD));
        for (String file : worldFolder.list(DIM_ONLY_FILTER))
        {
            if (!server.getDimensionMap().containsKey(file)) server.getDimensionMap().put(file, new Dimension(file));
        }
    }

    public String getWorldName()
    {
        return server.getProperties().getProperty("level-name", "world");
    }

    public void makeWorldBackup() throws BackupException
    {
        if (!checkSpace()) throw new BackupException("Out of diskspace.");
        doBackup(new File(new File(server.getBackupFolder(), WORLD), BACKUP_SDF.format(new Date()) + ".zip"), new File(server.getFolder(), getWorldName()), ACCEPT_NONE_FILTER);
    }

    public void makeAllOfTheBackup() throws BackupException
    {
        if (!checkSpace()) throw new BackupException("Out of diskspace.");
        doBackup(new File(new File(server.getBackupFolder(), SERVER), BACKUP_SDF.format(new Date()) + ".zip"), server.getFolder(), ACCEPT_NONE_FILTER);
    }

    public void makeBackup(String dimid) throws BackupException
    {
        if (!checkSpace()) throw new BackupException("Out of diskspace.");
        doBackup(new File(new File(server.getBackupFolder(), dimid), BACKUP_SDF.format(new Date()) + ".zip"), getFolder(dimid), dimid.equals(OVERWORLD) ? DIM_ONLY_FILTER : ACCEPT_NONE_FILTER);
    }

    public File getFolder(String dimid)
    {
        return dimid.equals(OVERWORLD) ? worldFolder : new File(worldFolder, dimid);
    }

    public void delete(String dimid) throws IOException
    {
        for (File file : getFolder(dimid).listFiles(dimid.equals(OVERWORLD) ? NOT_DIM_FILTER : ACCEPT_ALL_FILTER))
        {
            if (file.isFile()) file.delete();
            else if (file.isDirectory()) FileUtils.deleteDirectory(file);
        }
    }

    public void doBackup(File zip, File folder, FilenameFilter filter)
    {
        if (!folder.exists()) return; // Prevent derp
        LOGGER.info(String.format("'%s' is making a backup from '%s' to '%s'", server.getID(), folder.getPath(), zip.getPath()));
        if (server.getOnline())
        {
            server.sendCmd("say Making backup....");
            server.sendCmd("save-off");
            server.sendCmd("save-all");
        }
        try
        {
            File tmp = new File(server.getBackupFolder(), "tmp");
            if (tmp.exists()) FileUtils.deleteDirectory(tmp);
            tmp.mkdirs();
            FileUtils.copyDirectory(folder, tmp);
            zip.getParentFile().mkdirs();

            for (File delfolder : tmp.listFiles(filter)) FileUtils.deleteDirectory(delfolder);

            if (tmp.list().length != 0)
            {
                ZipParameters parameters = new ZipParameters();
                parameters.setIncludeRootFolder(false);
                ZipFile zipFile = new ZipFile(zip);
                zipFile.createZipFileFromFolder(tmp, parameters, false, 0);
            }

            if (tmp.exists()) FileUtils.deleteDirectory(tmp);
        }
        catch (IOException | ZipException e)
        {
            if (server.getOnline()) server.sendCmd("say Error when making backup");
            server.error(e);
        }
        if (server.getOnline())
        {
            server.sendCmd("save-on");
            server.sendCmd("save-all");
        }
    }

    private boolean checkSpace()
    {
        if (bypassLimits) return !(bypassLimits = false);
        return server.getOwnerObject().getMaxDiskspace() == -1 || server.getOwnerObject().getDiskspaceLeft() <= 0;
    }
}
