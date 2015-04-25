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

package net.doubledoordev.backend.util;

import com.google.gson.JsonElement;
import net.doubledoordev.backend.Main;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * To convert some string to a viable that fits into a target class.
 * Please don't use this code in anything important because it might blow up.
 * Also: it can't convert all data. Just basic stuff for now.
 *
 * @author Dries007
 */
public class TypeHellhole
{
    /**
     * Map containing target classes as key and a static method accepting just one string (aka "parse" methods)
     */
    static final Map<Class, Method> MAP = new HashMap<>();

    private TypeHellhole()
    {
    }

    /**
     * Only useful method in this class.
     *
     * @param clazz target
     * @param s     subject
     * @return Hopefully a casted version of the string.
     */
    public static Object convert(Class<?> clazz, String s)
    {
        if (MAP.containsKey(clazz)) try
        {
            return MAP.get(clazz).invoke(null, s);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            Main.LOGGER.warn("This should not happen. Ever!");
            e.printStackTrace();
        }
        return clazz.cast(s);
    }

    static
    {
        try
        {
            MAP.put(Boolean.class, Boolean.class.getMethod("parseBoolean", String.class));
            MAP.put(Integer.class, Integer.class.getMethod("parseInt", String.class));
            MAP.put(Long.class, Long.class.getMethod("parseLong", String.class));
            MAP.put(Float.class, Float.class.getMethod("parseFloat", String.class));
            MAP.put(Double.class, Double.class.getMethod("parseDouble", String.class));
            MAP.put(Byte.class, Byte.class.getMethod("parseByte", String.class));
            MAP.put(Short.class, Short.class.getMethod("parseShort", String.class));

            MAP.put(boolean.class, Boolean.class.getMethod("parseBoolean", String.class));
            MAP.put(int.class, Integer.class.getMethod("parseInt", String.class));
            MAP.put(long.class, Long.class.getMethod("parseLong", String.class));
            MAP.put(float.class, Float.class.getMethod("parseFloat", String.class));
            MAP.put(double.class, Double.class.getMethod("parseDouble", String.class));
            MAP.put(byte.class, Byte.class.getMethod("parseByte", String.class));
            MAP.put(short.class, Short.class.getMethod("parseShort", String.class));
        }
        catch (NoSuchMethodException e)
        {
            Main.LOGGER.error("TypeHellhole init went wrong...", e);
        }
    }

    public static void set(Field field, Object object, JsonElement value) throws Exception
    {
        if (field.getType() == byte.class) field.setByte(object, value.getAsByte());
        else if (field.getType() == short.class) field.setShort(object, value.getAsShort());
        else if (field.getType() == int.class) field.setInt(object, value.getAsInt());
        else if (field.getType() == long.class) field.setLong(object, value.getAsLong());
        else if (field.getType() == float.class) field.setFloat(object, value.getAsFloat());
        else if (field.getType() == double.class) field.setDouble(object, value.getAsDouble());
        else if (field.getType() == boolean.class) field.setBoolean(object, value.getAsBoolean());
        else if (field.getType() == char.class) field.setChar(object, value.getAsCharacter());
            //
        else if (field.getType() == Byte.class) field.set(object, value.getAsByte());
        else if (field.getType() == Short.class) field.set(object, value.getAsShort());
        else if (field.getType() == Integer.class) field.set(object, value.getAsInt());
        else if (field.getType() == Long.class) field.set(object, value.getAsLong());
        else if (field.getType() == Float.class) field.set(object, value.getAsFloat());
        else if (field.getType() == Double.class) field.set(object, value.getAsDouble());
        else if (field.getType() == Boolean.class) field.set(object, value.getAsBoolean());
        else if (field.getType() == Character.class) field.set(object, value.getAsCharacter());
            //
        else if (field.getType() == String.class) field.set(object, value.getAsString());
        else
        {
            String m = String.format("Unknown type! Field type: %s Json value: %s Data class: %s", field.getType(), value.toString(), object.getClass().getSimpleName());
            Main.LOGGER.error(m);
            throw new Exception(m);
        }
    }
}
