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

import net.doubledoordev.backend.util.exceptions.ServerOfflineException;

import java.net.*;

/**
 * A class that handles Minecraft Query protocol requests
 *
 * @author Ryan McCann
 */
public class MCQuery
{
    final static byte HANDSHAKE = 9;
    final static byte STAT = 0;

    String serverAddress = "localhost";
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
            e.printStackTrace();
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
