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
