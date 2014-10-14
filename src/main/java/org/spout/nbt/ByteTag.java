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

/**
 * The {@code TAG_Byte} tag.
 */
public final class ByteTag extends Tag<Byte>
{
    /**
     * The value.
     */
    private final byte value;

    /**
     * Creates the tag.<br> Boolean true is stored as 1 and boolean false is stored as 0.
     *
     * @param name  The name.
     * @param value The value.
     */
    public ByteTag(String name, boolean value)
    {
        this(name, (byte) (value ? 1 : 0));
    }

    /**
     * Creates the tag.
     *
     * @param name  The name.
     * @param value The value.
     */
    public ByteTag(String name, byte value)
    {
        super(TagType.TAG_BYTE, name);
        this.value = value;
    }

    public static Boolean getBooleanValue(Tag<?> t)
    {
        if (t == null)
        {
            return null;
        }
        try
        {
            ByteTag byteTag = (ByteTag) t;
            return byteTag.getBooleanValue();
        }
        catch (ClassCastException e)
        {
            return null;
        }
    }

    @Override
    public Byte getValue()
    {
        return value;
    }

    public ByteTag clone()
    {
        return new ByteTag(getName(), value);
    }

    public boolean getBooleanValue()
    {
        return value != 0;
    }

    @Override
    public String toString()
    {
        String name = getName();
        String append = "";
        if (name != null && !name.equals(""))
        {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Byte" + append + ": " + value;
    }
}
