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

package net.doubledoordev.backend.util;

import net.doubledoordev.backend.web.socket.ConsoleSocketApplication;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

/**
 * @author Dries007
 */
@Plugin(name = "CustomLogAppender", category = "Core", elementType = "appender", printObject = true)
public class CustomLogAppender extends AbstractAppender
{
    protected CustomLogAppender(String name, Filter filter, Layout<? extends Serializable> layout)
    {
        super(name, filter, layout);
    }

    protected CustomLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static CustomLogAppender createAppender(@PluginAttribute("name") String name, @PluginAttribute("ignoreExceptions") boolean ignoreExceptions, @PluginElement("Layout") Layout layout, @PluginElement("Filters") Filter filter)
    {
        if (name == null)
        {
            LOGGER.error("No name provided for StubAppender");
            return null;
        }

        if (layout == null)
        {
            layout = PatternLayout.createDefaultLayout();
        }
        //noinspection unchecked
        return new CustomLogAppender(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event)
    {
        ConsoleSocketApplication.sendLine(new String(getLayout().toByteArray(event)));
    }
}
