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

import fi.iki.elonen.NanoHTTPD;
import freemarker.template.*;
import net.doubledoordev.backend.Main;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Dries007
 */
public class PageResolver
{
    private static final Configuration freemarkerCfg = new Configuration();

    static
    {
        freemarkerCfg.setClassForTemplateLoading(Main.class, "/templates/");
        freemarkerCfg.setObjectWrapper(new DefaultObjectWrapper());
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        freemarkerCfg.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20
    }

    public static String resolve(String uri, NanoHTTPD.IHTTPSession session)
    {
        try
        {
            String[] args = uri.split("/");
            StringWriter stringWriter = new StringWriter();
            freemarkerCfg.getTemplate(getTemplateFor(args, session)).process(getDataForPage(args, session), stringWriter);
            return stringWriter.toString();
        }
        catch (TemplateException | IOException e)
        {
            e.printStackTrace();
            return String.format("<h1>Error!</h1><p>Message:<br><pre>%s</pre></p>", e.getLocalizedMessage());
        }
    }

    private static String getTemplateFor(String[] args, NanoHTTPD.IHTTPSession session)
    {
        switch (args[0])
        {
            default:
                return "index.html";

            case "servers":
                return "server.html";
            case "console":
                return "console.html";
        }
    }

    private static Object getDataForPage(String[] args, NanoHTTPD.IHTTPSession session)
    {
        switch (args[0])
        {
            default:
                return Settings.SETTINGS;

            case "console":
            case "servers":
                return Settings.getServerByName(args[1]);
        }
    }
}
