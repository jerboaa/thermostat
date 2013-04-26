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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.junit.Test;

import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;

public class PluginConfigurationParserTest {

    @Test(expected = PluginConfigurationParseException.class)
    public void testEmptyConfigurationThrowsException() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n";
        PluginConfigurationParser parser = new PluginConfigurationParser();
        parser.parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));
        fail("should not reach here");
    }

    @Test
    public void testMinimalConfiguration() throws UnsupportedEncodingException {
        PluginConfigurationParser parser = new PluginConfigurationParser();
        String config = "" +
                "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "</plugin>";
        PluginConfiguration result = parser.parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());
        assertEquals(0, result.getNewCommands().size());
    }

    @Test
    public void testConfigurationThatExtendsExistingCommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <extensions>\n" +
                "    <extension>\n" +
                "      <name>test</name>\n" +
                "      <bundles>\n" +
                "        <bundle>foo</bundle>\n" +
                "        <bundle>bar</bundle>\n" +
                "        <bundle>baz</bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n" +
                "        <dependency>thermostat-foo</dependency>\n" +
                "      </dependencies>\n" +
                "    </extension>\n" +
                "  </extensions>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getNewCommands().size());

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(1, extensions.size());

        CommandExtensions first = extensions.get(0);
        assertEquals("test", first.getCommandName());
        assertEquals(Arrays.asList("foo", "bar", "baz"), first.getPluginBundles());
        assertEquals(Arrays.asList("thermostat-foo"), first.getDepenedencyBundles());
    }

    @Test
    public void testConfigurationThatAddsNewCommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <description>description</description>\n" +
                "      <bundles>\n" +
                "        <bundle>foo</bundle>\n" +
                "        <bundle>bar</bundle>\n" +
                "        <bundle>baz</bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n" +
                "        <dependency>thermostat-foo</dependency>\n" +
                "      </dependencies>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(0, extensions.size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand newCommand = newCommands.get(0);
        assertEquals("test", newCommand.getCommandName());
        assertEquals("description", newCommand.getDescription());
        Options opts = newCommand.getOptions();
        assertTrue(opts.getOptions().isEmpty());
        assertTrue(opts.getRequiredOptions().isEmpty());
        assertEquals(Arrays.asList("foo", "bar", "baz"), newCommand.getPluginBundles());
        assertEquals(Arrays.asList("thermostat-foo"), newCommand.getDepenedencyBundles());
    }

    @Test
    public void testSpacesAtStartAndEndAreTrimmed() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <extensions>" +
                "    <extension>\n" +
                "      <name>\ntest   \n</name>\n" +
                "      <bundles>\n" +
                "        <bundle>\n \t  \nfoo\t \n \n</bundle>\n" +
                "        <bundle>\tbar  baz\n</bundle>\n" +
                "        <bundle>buzz</bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n\t\n\t \t\t\n" +
                "        <dependency>\t\t\t  thermostat-foo\n\t\t\n</dependency>\n" +
                "      </dependencies>\n" +
                "    </extension>\n" +
                "  </extensions>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getNewCommands().size());

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(1, extensions.size());

        CommandExtensions first = extensions.get(0);
        assertEquals("test", first.getCommandName());
        assertEquals(Arrays.asList("foo", "bar  baz", "buzz"), first.getPluginBundles());
        assertEquals(Arrays.asList("thermostat-foo"), first.getDepenedencyBundles());
    }

    @Test
    public void testArgumentParsing() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <description>just a test</description>\n" +
                "      <arguments>\n" +
                "        <argument>file</argument>\n" +
                "      </arguments>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
        assertEquals("just a test", command.getDescription());
        assertEquals(null, command.getUsage());
        Options opts = command.getOptions();
        assertTrue(opts.getOptions().isEmpty());

        List<String> args = command.getPositionalArguments();
        assertEquals(1, args.size());
        assertEquals("file", args.get(0));
    }

    @Test
    public void testOptionParsing() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <group>\n" +
                "          <required>true</required>\n" +
                "          <option>\n" +
                "            <long>exclusive-a</long>\n" +
                "            <short>a</short>\n" +
                "            <required>false</required>\n" +
                "            <description>exclusive option a</description>\n" +
                "          </option>\n" +
                "          <option>\n" +
                "            <long>exclusive-b</long>\n" +
                "            <short>b</short>\n" +
                "            <required>false</required>\n" +
                "            <description>exclusive option b</description>\n" +
                "          </option>\n" +
                "        </group>\n" +
                "        <option>\n" +
                "          <long>long</long>\n" +
                "          <short>l</short>\n" +
                "          <argument>name</argument>\n" +
                "          <required>true</required>\n" +
                "          <description>some required and long option</description>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
        assertEquals("just a test", command.getDescription());
        Options opts = command.getOptions();
        assertNull(opts.getOption("foobarbaz"));

        Option requiredOption = opts.getOption("l");
        assertNotNull(requiredOption);

        Option exclusiveOptionA = opts.getOption("a");
        assertNotNull(exclusiveOptionA);
        assertEquals("exclusive-a", exclusiveOptionA.getLongOpt());
        assertFalse(exclusiveOptionA.hasArg());
        assertFalse(exclusiveOptionA.isRequired());
        assertEquals("exclusive option a", exclusiveOptionA.getDescription());

        Option exclusiveOptionB = opts.getOption("b");
        assertNotNull(exclusiveOptionB);
        assertEquals("exclusive-b", exclusiveOptionB.getLongOpt());
        assertFalse(exclusiveOptionB.hasArg());
        assertFalse(exclusiveOptionB.isRequired());
        assertEquals("exclusive option b", exclusiveOptionB.getDescription());

        OptionGroup group = opts.getOptionGroup(exclusiveOptionA);
        assertTrue(group.isRequired());
    }

    @Test
    public void testCommonOptionParsing() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <option common=\"true\">\n" +
                "          <long>dbUrl</long>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);

        Options opts = command.getOptions();
        assertTrue(opts.getRequiredOptions().isEmpty());

        Option dbUrlOption = opts.getOption("d");
        assertNotNull(dbUrlOption);

        Option otherDbUrlOption = opts.getOption("dbUrl");
        assertSame(dbUrlOption, otherDbUrlOption);

        Translate<LocaleResources> t = LocaleResources.createLocalizer();

        assertEquals("dbUrl", dbUrlOption.getArgName());
        assertEquals(1, dbUrlOption.getArgs());
        assertEquals(t.localize(LocaleResources.OPTION_DB_URL_DESC), dbUrlOption.getDescription());
        assertFalse(dbUrlOption.isRequired());
    }

    @Test
    public void testFakeCommonOptionIsIgnored() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <option common=\"true\">\n" +
                "          <long>foobarbaz</long>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);

        Options opts = command.getOptions();
        assertTrue(opts.getRequiredOptions().isEmpty());

        Option dbUrlOption = opts.getOption("foobarbaz");
        assertNull(dbUrlOption);
    }
}
