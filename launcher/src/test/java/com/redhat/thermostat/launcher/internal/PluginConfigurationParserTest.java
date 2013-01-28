/*
 * Copyright 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;

public class PluginConfigurationParserTest {

    @Test(expected = PluginConfigurationParseException.class)
    public void testEmptyConfigurationThrowsException() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n";
        PluginConfigurationParser parser = new PluginConfigurationParser();
        parser.parse(new ByteArrayInputStream(config.getBytes("UTF-8")));
        fail("should not reach here");
    }

    @Test
    public void testMinimalConfiguration() throws UnsupportedEncodingException {
        PluginConfigurationParser parser = new PluginConfigurationParser();
        String config = "" +
                "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "</plugin>";
        PluginConfiguration result = parser.parse(new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());
        assertEquals(0, result.getNewCommands().size());
    }

    @Test
    public void testConfigurationThatExtendsExistingCommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <existing>\n" +
                "      <name>test</name>\n" +
                "      <bundles>foo,bar,baz,</bundles>\n" +
                "      <dependencies>thermostat-foo</dependencies>\n" +
                "    </existing>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse(new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getNewCommands().size());

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(1, extensions.size());

        CommandExtensions first = extensions.get(0);
        assertEquals("test", first.getCommandName());
        assertEquals(Arrays.asList("foo", "bar", "baz"), first.getAdditionalBundles());
        assertEquals(Arrays.asList("thermostat-foo"), first.getDepenedencyBundles());
    }

    @Test
    public void testConfigurationThatAddsNewCommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <new>\n" +
                "      <name>test</name>\n" +
                "      <usage>usage: test</usage>\n" +
                "      <description>description</description>\n" +
                "      <bundles>foo,bar,baz,</bundles>\n" +
                "      <dependencies>thermostat-foo</dependencies>\n" +
                "    </new>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse(new ByteArrayInputStream(config.getBytes("UTF-8")));

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(0, extensions.size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand newCommand = newCommands.get(0);
        assertEquals("test", newCommand.getCommandName());
        assertEquals("usage: test", newCommand.getUsage());
        assertEquals("description", newCommand.getDescription());
        assertEquals(null, newCommand.getOptions());
        assertEquals(Arrays.asList("foo", "bar", "baz"), newCommand.getAdditionalBundles());
        assertEquals(Arrays.asList("thermostat-foo"), newCommand.getCoreDepenedencyBundles());
    }

}
