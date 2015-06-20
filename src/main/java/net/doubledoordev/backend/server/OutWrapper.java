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

import net.doubledoordev.backend.util.methodCaller.IMethodCaller;

import java.io.PrintStream;

/**
 * @author Dries007
 */
public class OutWrapper extends PrintStream
{
    private final IMethodCaller methodCaller;
    private final Server server;

    public OutWrapper(PrintStream oldPrintWriter, IMethodCaller methodCaller, Server server)
    {
        super(oldPrintWriter);
        this.methodCaller = methodCaller;
        this.server = server;
    }

    @Override
    public void println()
    {
        server.printLine("");
        methodCaller.sendMessage("");
    }

    @Override
    public void println(boolean x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(char x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(int x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(long x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(float x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(double x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(char[] x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }

    @Override
    public void println(String x)
    {
        server.printLine(x);
        methodCaller.sendMessage(x);
    }

    @Override
    public void println(Object x)
    {
        server.printLine(String.valueOf(x));
        methodCaller.sendMessage(String.valueOf(x));
    }
}
