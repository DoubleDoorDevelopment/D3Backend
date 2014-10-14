/*
 * This file is part of SimpleNBT.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * SimpleNBT is licensed under the Spout License Version 1.
 *
 * SimpleNBT is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * SimpleNBT is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.nbt;

import java.util.Arrays;

/**
 * The {@code TAG_Byte_Array} tag.
 */
public final class ByteArrayTag extends Tag<byte[]>
{
    /**
     * The value.
     */
    private final byte[] value;

    /**
     * Creates the tag.
     *
     * @param name  The name.
     * @param value The value.
     */
    public ByteArrayTag(String name, byte[] value)
    {
        super(TagType.TAG_BYTE_ARRAY, name);
        this.value = value;
    }

    @Override
    public byte[] getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ByteArrayTag))
        {
            return false;
        }

        ByteArrayTag tag = (ByteArrayTag) other;
        return Arrays.equals(value, tag.value) && getName().equals(tag.getName());
    }

    public ByteArrayTag clone()
    {
        byte[] clonedArray = cloneArray(value);

        return new ByteArrayTag(getName(), clonedArray);
    }

    @Override
    public String toString()
    {
        StringBuilder hex = new StringBuilder();
        for (byte b : value)
        {
            String hexDigits = Integer.toHexString(b).toUpperCase();
            if (hexDigits.length() == 1)
            {
                hex.append("0");
            }
            hex.append(hexDigits).append(" ");
        }

        String name = getName();
        String append = "";
        if (name != null && !name.equals(""))
        {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Byte_Array" + append + ": " + hex.toString();
    }

    private byte[] cloneArray(byte[] byteArray)
    {
        if (byteArray == null)
        {
            return null;
        }
        else
        {
            int length = byteArray.length;
            byte[] newArray = new byte[length];
            System.arraycopy(byteArray, 0, newArray, 0, length);
            return newArray;
        }
    }
}
