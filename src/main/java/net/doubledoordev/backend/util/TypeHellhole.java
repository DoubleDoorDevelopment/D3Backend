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

package net.doubledoordev.backend.util;

import net.doubledoordev.backend.Main;

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
     * @throws Exception because it will go wrong.
     */
    public static Object convert(Class<?> clazz, String s) throws Exception
    {
        if (MAP.containsKey(clazz)) return MAP.get(clazz).invoke(null, s);
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
}
