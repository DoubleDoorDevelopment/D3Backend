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

import com.flowpowered.nbt.Tag;
import net.doubledoordev.backend.util.Helper;
import net.doubledoordev.backend.util.JsonNBTHelper;
import net.doubledoordev.backend.util.methodCaller.IMethodCaller;
import net.doubledoordev.backend.web.socket.FileManagerSocketApplication;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.util.Strings;
import org.glassfish.grizzly.http.util.MimeType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Current limitations:
 * You can't make a new NBT file, as it will not be able to parse the compression state.
 *
 * @author Dries007
 */
@SuppressWarnings("UnusedDeclaration")
public class FileManager
{
    private final Server server;
    private final File serverFolder;
    private final File file;

    public FileManager(Server server, String fileString)
    {
        this.server = server;
        this.serverFolder = server.getFolder();
        File file = this.serverFolder;

        if (fileString != null && !fileString.trim().isEmpty() && !serverFolder.getName().equals(fileString))
        {
            fileString = fileString.replace("/../", "").replace("\\..\\", "");
            try
            {
                file = new File(this.serverFolder, fileString).getCanonicalFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        this.file = file;

        if (this.file == null) throw new NullPointerException("File was null! Server:" + server + " FileString: " + fileString);
    }

    public Server getServer()
    {
        return server;
    }

    public File getFile()
    {
        return file;
    }

    public String getExtension()
    {
        return getExtension(file);
    }

    public String getExtension(File file)
    {
        return FilenameUtils.getExtension(file.getName().toLowerCase());
    }

    public Collection<File> makeBreadcrumbs()
    {
        LinkedList<File> list = new LinkedList<>();

        File crumb = file;
        while (!crumb.equals(serverFolder))
        {
            list.add(crumb);
            crumb = crumb.getParentFile();
        }

        list.add(serverFolder);

        Collections.reverse(list);
        return list;
    }

    public String stripServer(File file)
    {
        if (file.equals(serverFolder)) return serverFolder.getName();
        return file.toString().substring(serverFolder.toString().length() + 1).replace('\\', '/');
    }

    public boolean canEdit(File file)
    {
        switch (getExtension(file))
        {
            case "jar":
            case "zip":
            case "disabled":
            case "exe":
            case "mca":
            case "mcr":
                return false;

            default:
                return true;
        }
    }

    public String getEditor()
    {
        if (file.getName().equals("ops.json")) return "ops.ftl";
        if (file.getName().equals("whitelist.json")) return "whitelist.ftl";
        if (file.getName().equals("banned-players.json")) return "banned-players.ftl";
        if (file.getName().equals("banned-ips.json")) return "banned-ips.ftl";
        if (file.getName().equals("server.properties")) return "serverProperties.ftl";
        switch (getExtension())
        {
            case "jar":
            case "zip":
            case "disabled":
            case "mca":
            case "mcr":
                return null;

            case "json":
            case "dat":
            case "dat_old":
                return "json.ftl";

            case "jpg":
            case "png":
                return "img.ftl";

            default:
                return "ace.ftl";
        }
    }

    public String getIcon(File file)
    {
        if (!file.canWrite()) return "lock";
        if (file.isDirectory()) return "folder";
        switch (getExtension(file))
        {
            case "html":
            case "json":
            case "dat":
            case "dat_old":
            case "properties":
                return "file-code-o";

            case "txt":
                return "file-text-o";

            case "jar":
            case "zip":
            case "disabled":
                return "file-archive-o";

            case "jpg":
            case "png":
                return "file-image-o";

            default:
                return "file-o";
        }
    }

    public String getFileContents() throws IOException
    {
        switch (getExtension())
        {
            case "json":
                return FileUtils.readFileToString(file);
            case "dat":
            case "dat_old":

                Tag tag = Helper.readRawNBT(file, true);
                if (tag == null) tag = Helper.readRawNBT(file, false);
                if (tag != null)
                {
                    return JsonNBTHelper.parseNBT(tag).toString();
                }
                else
                {
                    return FileUtils.readFileToString(file);
                }
            case "jpg":
            case "png":
                return String.format("data:%s;base64,%s", MimeType.get(getExtension()), Base64.encodeBase64String(FileUtils.readFileToByteArray(file)));
            case "jar":
            case "zip":
            case "disabled":
            case "mca":
            case "mcr":
                return null;
            default:
                return StringEscapeUtils.escapeHtml4(FileUtils.readFileToString(file));
        }
    }

    public void rename(String newname)
    {
        file.renameTo(new File(file.getParentFile(), newname));
    }

    public void makeWritable()
    {
        file.setWritable(true);
    }

    public void delete() throws IOException
    {
        if (file.isDirectory()) FileUtils.deleteDirectory(file);
        else file.delete();
    }

    public void newFile(String name) throws IOException
    {
        new File(file, name).createNewFile();
    }

    public void newFolder(String name)
    {
        new File(file, name).mkdir();
    }

    public void set(IMethodCaller caller, String text) throws IOException
    {
        FileUtils.writeStringToFile(file, text);
        FileManagerSocketApplication.sendFile(file.getAbsolutePath(), text);
    }

    public String getSize(File file)
    {
        long sizeL = file.length();
        if (sizeL < 1000) return String.format("%d B", sizeL);
        double sizeD = sizeL / 1000D;
        if (sizeD < 1000) return String.format("%.2f kB", sizeD);
        sizeD = sizeL / 1000000D;
        if (sizeD < 1000) return String.format("%.2f MB", sizeD);
        sizeD = sizeL / 1000000000D;
        return String.format("%.2f GB", sizeD);
    }
}
