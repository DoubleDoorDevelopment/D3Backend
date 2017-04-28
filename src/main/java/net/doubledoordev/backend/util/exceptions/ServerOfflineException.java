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

package net.doubledoordev.backend.util.exceptions;

/**
 * Throws when server was offline.
 * Used in MCQuery.
 *
 * @author Dries007
 */
public class ServerOfflineException extends Exception
{
    public ServerOfflineException()
    {
    }

    public ServerOfflineException(String message)
    {
        super(message);
    }

    public ServerOfflineException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ServerOfflineException(Throwable cause)
    {
        super(cause);
    }

    public ServerOfflineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
