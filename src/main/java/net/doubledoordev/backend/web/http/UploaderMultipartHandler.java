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

package net.doubledoordev.backend.web.http;

import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOInputStream;
import org.glassfish.grizzly.http.multipart.ContentDisposition;
import org.glassfish.grizzly.http.multipart.MultipartEntry;
import org.glassfish.grizzly.http.multipart.MultipartEntryHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Dries007
 */
public class UploaderMultipartHandler implements MultipartEntryHandler
{
    private static final String FILENAME_ENTRY = "fileName";
    private final File folder;

    public UploaderMultipartHandler(File folder)
    {
        this.folder = folder;
    }

    @Override
    public void handle(final MultipartEntry multipartEntry) throws Exception
    {
        final ContentDisposition contentDisposition = multipartEntry.getContentDisposition();
        final String name = contentDisposition.getDispositionParamUnquoted("name");

        // If part contains file
        if (name.equals(FILENAME_ENTRY))
        {
            final String filename = contentDisposition.getDispositionParamUnquoted("filename");
            final NIOInputStream inputStream = multipartEntry.getNIOInputStream();

            // start asynchronous non-blocking content read.
            inputStream.notifyAvailable(new UploadReadHandler(new File(folder, filename), inputStream));
        }
        else
        {
            multipartEntry.skip();
        }
    }

    /**
     * Simple {@link org.glassfish.grizzly.ReadHandler} implementation, which is reading HTTP request
     * content (uploading file) in non-blocking mode and saves the content into
     * the specific file.
     */
    private static class UploadReadHandler implements ReadHandler
    {
        private final NIOInputStream inputStream;
        private final FileOutputStream fileOutputStream;
        private final byte[] buf;

        private UploadReadHandler(final File file, final NIOInputStream inputStream) throws FileNotFoundException
        {
            fileOutputStream = new FileOutputStream(file);
            this.inputStream = inputStream;
            buf = new byte[2048];
        }

        @Override
        public void onDataAvailable() throws Exception
        {
            readAndSaveAvail();
            inputStream.notifyAvailable(this);
        }

        @Override
        public void onAllDataRead() throws Exception
        {
            readAndSaveAvail();
            finish();
        }

        @Override
        public void onError(Throwable t)
        {
            finish();
        }

        private void readAndSaveAvail() throws IOException
        {
            while (inputStream.isReady())
            {
                fileOutputStream.write(buf, 0, inputStream.read(buf));
            }
        }

        private void finish()
        {
            try
            {
                fileOutputStream.close();
            }
            catch (IOException ignored)
            {
            }
        }
    }
}
