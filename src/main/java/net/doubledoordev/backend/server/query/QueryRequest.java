/*
 * Unless otherwise specified through the '@author' tag or the javadoc
 * at the top of the file or on a specific portion of the code the following license applies:
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

package net.doubledoordev.backend.server.query;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Ryan McCann
 */
public class QueryRequest
{
    static byte[] MAGIC = {(byte) 0xFE, (byte) 0xFD};
    byte type;
    int sessionID;
    byte[] payload;
    private ByteArrayOutputStream byteStream;
    private DataOutputStream dataStream;

    public QueryRequest()
    {
        int size = 1460;
        byteStream = new ByteArrayOutputStream(size);
        dataStream = new DataOutputStream(byteStream);
    }

    public QueryRequest(byte type)
    {
        this.type = type;
    }

    //convert the data in this request to a byte array to send to the server
    byte[] toBytes()
    {
        byteStream.reset();

        try
        {
            dataStream.write(MAGIC);
            dataStream.write(type);
            dataStream.writeInt(sessionID);
            dataStream.write(payloadBytes());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return byteStream.toByteArray();
    }

    private byte[] payloadBytes()
    {
        if (type == MCQuery.HANDSHAKE)
        {
            return new byte[]{}; //return empty byte array
        }
        else //(type == MCQuery.STAT)
        {
            return payload;
        }
    }

    protected void setPayload(int load)
    {
        this.payload = ByteUtils.intToBytes(load);
    }
}
