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

package net.doubledoordev.backend.util.winreg;

/**
 * Java Finder by petrucio@stackoverflow(828681) is licensed under a Creative Commons Attribution 3.0 Unported License.
 * Needs WinRegistry.java. Get it at: http://stackoverflow.com/questions/62289/read-write-to-windows-registry-using-java
 *
 * JavaFinder - Windows-specific classes to search for all installed versions of java on this system
 * Author: petrucio@stackoverflow (828681)
 *****************************************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Helper class to fetch the stdout and stderr outputs from started Runtime execs
 * Modified from http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
 * ***************************************************************************
 */
class RuntimeStreamer extends Thread
{
    InputStream is;
    String lines;

    RuntimeStreamer(InputStream is)
    {
        this.is = is;
        this.lines = "";
    }

    /**
     * Execute a command and wait for it to finish
     *
     * @return The resulting stdout and stderr outputs concatenated
     * **************************************************************************
     */
    public static String execute(String[] cmdArray)
    {
        try
        {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(cmdArray);
            RuntimeStreamer outputStreamer = new RuntimeStreamer(proc.getInputStream());
            RuntimeStreamer errorStreamer = new RuntimeStreamer(proc.getErrorStream());
            outputStreamer.start();
            errorStreamer.start();
            proc.waitFor();
            return outputStreamer.contents() + errorStreamer.contents();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return null;
    }

    public static String execute(String cmd)
    {
        String[] cmdArray = {cmd};
        return RuntimeStreamer.execute(cmdArray);
    }

    public String contents()
    {
        return this.lines;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
            {
                this.lines += line + "\n";
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}
