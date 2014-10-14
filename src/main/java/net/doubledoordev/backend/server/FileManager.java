/*
 * Unless otherwise specified through the '@author' tag or comments at
 * the top of the file or on a specific portion of the code the following license applies:
 *
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  The header specified or the above copyright notice, this list of conditions
 *   and the following disclaimer below must be displayed at the top of the source code
 *   of any web page received while using any part of the service this software provides.
 *
 *   The header to be displayed:
 *       This page was generated by DoubleDoorDevelopment's D3Backend or a derivative thereof.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.backend.server;

import net.doubledoordev.backend.webserver.NanoHTTPD;
import net.doubledoordev.backend.webserver.SimpleWebServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static net.doubledoordev.backend.webserver.NanoHTTPD.MIME_PLAINTEXT;
import static net.doubledoordev.backend.webserver.NanoHTTPD.Response.Status.FORBIDDEN;
import static net.doubledoordev.backend.webserver.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static net.doubledoordev.backend.webserver.NanoHTTPD.Response.Status.OK;

/**
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

        if (Strings.isNotBlank(fileString) && !fileString.equals(serverFolder.getName()))
        {
            fileString = fileString.replace("..", "");
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
        return file.toString().substring(serverFolder.toString().length() + 1);
    }

    public boolean canEdit(File file)
    {
        switch (getExtension(file))
        {
            case "jar":
            case "zip":
            case "disabled":
                return false;

            default:
                return true;
        }
    }

    public String getEditor()
    {
        switch (getExtension())
        {
            case "jar":
            case "zip":
            case "disabled":
                return null;

            case "json":
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

    public String getRawFileContents() throws IOException
    {
        return FileUtils.readFileToString(file);
    }

    public String getFileContentsAsString() throws IOException
    {
        return StringEscapeUtils.escapeHtml4(FileUtils.readFileToString(file));
    }

    public String getFileContentsAsBase64() throws IOException
    {
        return String.format("data:%s;base64,%s", SimpleWebServer.MIME_TYPES.get(getExtension()), Base64.encodeBase64String(FileUtils.readFileToByteArray(file)));
    }

    public NanoHTTPD.Response set(String contents)
    {
        if (!file.canWrite()) return new NanoHTTPD.Response(FORBIDDEN, MIME_PLAINTEXT, "File is write protected.");
        try
        {
            FileUtils.writeStringToFile(file, contents);
        }
        catch (IOException e)
        {
            return new NanoHTTPD.Response(INTERNAL_ERROR, MIME_PLAINTEXT, e.toString());
        }
        return new NanoHTTPD.Response(OK, MIME_PLAINTEXT, "OK");
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
}
