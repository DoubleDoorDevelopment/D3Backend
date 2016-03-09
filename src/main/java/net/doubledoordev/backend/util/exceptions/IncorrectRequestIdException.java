/*
 * D3Backend
 * Copyright (C) 2015 - 2016  Dries007 & Double Door Development
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
 * https://code.google.com/p/rcon-client/source/browse/trunk/RConClient/src/org/minecraft/rconclient/rcon/?r=2
 *
 * @author vincent
 */
public class IncorrectRequestIdException extends AuthenticationException
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception with the specified request id.
     *
     * @param requestId The request id.
     */
    public IncorrectRequestIdException(final int requestId)
    {
        super("Request id:" + requestId);
    }

    /**
     * Construct a new exception with the specified request id and cause.
     *
     * @param requestId The request id.
     * @param cause     The original cause of this exception.
     */
    public IncorrectRequestIdException(final int requestId, final Throwable cause)
    {
        super("Request id:" + requestId, cause);
    }
}
