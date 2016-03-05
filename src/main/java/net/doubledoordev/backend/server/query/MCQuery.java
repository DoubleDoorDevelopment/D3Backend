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

import net.doubledoordev.backend.Main;
import net.doubledoordev.backend.util.exceptions.ServerOfflineException;

import java.net.*;

import static net.doubledoordev.backend.util.Constants.LOCALHOST;

/**
 * A class that handles Minecraft Query protocol requests
 *
 * @author Ryan McCann
 */
public class MCQuery
{
    final static byte HANDSHAKE = 9;
    final static byte STAT = 0;

    String serverAddress = LOCALHOST;
    int queryPort = 25565; // the default minecraft query port

    int localPort = 25566; // the local port we're connected to the server on

    private DatagramSocket socket = null; //prevent socket already bound exception
    private int token;

    public MCQuery(String address, int port)
    {
        serverAddress = address;
        queryPort = port;
    }

    // used to get a session token
    private void handshake() throws ServerOfflineException
    {
        QueryRequest req = new QueryRequest();
        req.type = HANDSHAKE;
        req.sessionID = generateSessionID();

        int val = 11 - req.toBytes().length; //should be 11 bytes total
        byte[] input = ByteUtils.padArrayEnd(req.toBytes(), val);
        byte[] result = sendUDP(input);

        token = Integer.parseInt(new String(result).trim());
    }

    /**
     * Use this to get basic status information from the server.
     *
     * @return a <code>QueryResponse</code> object
     */
    public QueryResponse basicStat()
    {
        try
        {
            handshake(); //get the session token first

            QueryRequest req = new QueryRequest(); //create a request
            req.type = STAT;
            req.sessionID = generateSessionID();
            req.setPayload(token);
            byte[] send = req.toBytes();

            byte[] result = sendUDP(send);

            QueryResponse res = new QueryResponse(result, false);
            return res;
        }
        catch (ServerOfflineException e)
        {
            return null;
        }
    }

    /**
     * Use this to get more information, including players, from the server.
     *
     * @return a <code>QueryResponse</code> object
     */
    public QueryResponse fullStat()
    {
        try
        {
            handshake();

            QueryRequest req = new QueryRequest();
            req.type = STAT;
            req.sessionID = generateSessionID();
            req.setPayload(token);
            req.payload = ByteUtils.padArrayEnd(req.payload, 4); //for full stat, pad the payload with 4 null bytes

            byte[] send = req.toBytes();

            byte[] result = sendUDP(send);

            /*
             * note: buffer size = base + #players(online) * 16(max username length)
             */

            QueryResponse res = new QueryResponse(result, true);
            return res;
        }
        catch (ServerOfflineException e)
        {
            return null;
        }
        catch (Exception e)
        {
            Main.LOGGER.catching(e);
            return null;
        }
    }

    private byte[] sendUDP(byte[] input) throws ServerOfflineException
    {
        try
        {
            while (socket == null)
            {
                try
                {
                    socket = new DatagramSocket(localPort); //create the socket
                }
                catch (BindException e)
                {
                    ++localPort; // increment if port is already in use
                }
            }

            //create a packet from the input data and send it on the socket
            InetAddress address = InetAddress.getByName(serverAddress); //create InetAddress object from the address
            DatagramPacket packet1 = new DatagramPacket(input, input.length, address, queryPort);
            socket.send(packet1);

            //receive a response in a new packet
            byte[] out = new byte[1024];
            DatagramPacket packet = new DatagramPacket(out, out.length);
            socket.setSoTimeout(500); //one half second timeout
            socket.receive(packet);

            return packet.getData();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
        catch (SocketTimeoutException e)
        {
            //System.err.println("Socket Timeout! Is the server offline?");
            //System.exit(1);
            throw new ServerOfflineException(e);
        }
        catch (Exception e) //any other exceptions that may occur
        {
            Main.LOGGER.catching(e);
        }

        return null;
    }

    private int generateSessionID()
    {
        return 1;
    }

    @Override
    public void finalize()
    {
        socket.close();
    }
}
