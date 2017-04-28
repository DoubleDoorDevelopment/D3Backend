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

package net.doubledoordev.backend.util;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class for file downloading
 * Make sure you don't thread this twice. It happens once internally already!
 * <p/>
 * http://stackoverflow.com/questions/14069848/download-a-file-while-also-updating-a-jprogressbar
 *
 * @author Kevin Esche, Stackoverflow user.
 */
public class Download implements Runnable
{
    // Max size of download buffer.
    private static final int MAX_BUFFER_SIZE = 1024;
    private final File target;// target file
    private final URL url; // download URL
    private String message = "OK";
    private long size; // size of download in bytes
    private long downloaded; // number of bytes downloaded
    private Status status; // current status of download

    // Constructor for Download.
    public Download(URL url, File target)
    {
        this.target = target;
        this.url = url;
        size = -1;
        downloaded = 0;
        status = Status.Downloading;

        // Begin the download.
        download();
    }

    // Get this download's URL.
    public String getUrl()
    {
        return url.toString();
    }

    // Get this download's size.
    public long getSize()
    {
        return size;
    }

    // Get this download's progress.
    public float getProgress()
    {
        return ((float) downloaded / size) * 100;
    }

    public long getDownloaded()
    {
        return downloaded;
    }

    // Get this download's status.
    public Status getStatus()
    {
        return status;
    }

    // Mark this download as having an error.
    private void error(String message)
    {
        status = Status.Error;
        this.message = message;
    }

    // Start or resume downloading.
    private void download()
    {
        Thread thread = new Thread(this);
        thread.start();
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    // Download file.
    public void run()
    {
        RandomAccessFile file = null;
        InputStream stream = null;

        try
        {
            // Open connection to URL.
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Specify what portion of file to download.
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

            // Connect to server.
            connection.connect();

            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2)
            {
                error("Responce wasn't 200: " + connection.getResponseMessage());
            }

            // Check for valid content length.
            int contentLength = connection.getContentLength();
            if (contentLength < 1)
            {
                error("Content length is invalid.");
            }

            // Set the size for this download if it hasn't been already set.
            if (size == -1)
            {
                size = contentLength;
            }

            // Open file and seek to the end of it.
            file = new RandomAccessFile(target, "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == Status.Downloading)
            {
                // Size buffer according to how much of the file is left to download.
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE)
                {
                    buffer = new byte[MAX_BUFFER_SIZE];
                }
                else
                {
                    buffer = new byte[(int) (size - downloaded)];
                }

                // Read from server into buffer.
                int read = stream.read(buffer);
                if (read == -1) break;

                // Write buffer to file.
                file.write(buffer, 0, read);
                downloaded += read;
            }

            // Change status to complete if this point was reached because downloading has finished.
            if (status == Status.Downloading)
            {
                status = Status.Complete;
            }
        }
        catch (Exception e)
        {
            error(e.toString());
            e.printStackTrace();
        }
        finally
        {
            // Close file.
            if (file != null)
            {
                try
                {
                    file.close();
                }
                catch (Exception e)
                {
                }
            }

            // Close connection to server.
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    public static enum Status
    {
        Downloading, Complete, Error
    }
}
