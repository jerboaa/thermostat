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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;

public class PluginCommandInfoSourceTest {

    // name paths so anything tying to use them/create them will blow up
    private static final String PLUGIN_ROOT = "/fake/${PLUGIN_ROOT}";
    private static final String JAR_ROOT = "/fake/${JAR_ROOT}";
    private File jarRootDir;
    private File pluginRootDir;
    private PluginConfigurationParser parser;
    private PluginConfiguration parserResult;

    @Before
    public void setUp() throws FileNotFoundException {
        parser = mock(PluginConfigurationParser.class);
        parserResult = mock(PluginConfiguration.class);
        when(parser.parse(isA(File.class))).thenReturn(parserResult);
        pluginRootDir = mock(File.class);
        jarRootDir = new File(JAR_ROOT);
    }

    @Test
    public void verifyParserIsInvokedOnAllConfigurationFiles() throws FileNotFoundException {
        File[] pluginDirs = new File[] {
                new File(PLUGIN_ROOT, "plugin1"),
                new File(PLUGIN_ROOT, "plugin2"),
        };

        when(pluginRootDir.listFiles()).thenReturn(pluginDirs);

        PluginCommandInfoSource source = new PluginCommandInfoSource(jarRootDir, pluginRootDir, parser);

        ArgumentCaptor<File> configFilesCaptor = ArgumentCaptor.forClass(File.class);
        verify(parser, times(pluginDirs.length)).parse(configFilesCaptor.capture());

        List<File> configurationFiles = configFilesCaptor.getAllValues();
        assertEquals(pluginDirs.length, configurationFiles.size());
        for (int i = 0; i < pluginDirs.length; i++) {
            assertEquals(new File(pluginDirs[i], "plugin.conf"), configurationFiles.get(i));
        }
    }

    @Test
    public void verifyMissingConfigurationFileIsHandledCorrectly() throws FileNotFoundException {
        File[] pluginDirs = new File[] { new File(PLUGIN_ROOT, "plugin1") };

        when(pluginRootDir.listFiles()).thenReturn(pluginDirs);
        when(parser.parse(isA(File.class))).thenThrow(new FileNotFoundException("test"));

        PluginCommandInfoSource source = new PluginCommandInfoSource(jarRootDir, pluginRootDir, parser);
    }

    @Test
    public void verifyCommandInfoObjectsToExtendExistingCommandsAreCreated() {
        CommandExtensions extensions = mock(CommandExtensions.class);
        when(extensions.getCommandName()).thenReturn("command-name");
        when(extensions.getAdditionalBundles()).thenReturn(Arrays.asList("additional-bundle"));
        when(extensions.getDepenedencyBundles()).thenReturn(Arrays.asList("dependency-bundle"));

        when(parserResult.getExtendedCommands()).thenReturn(Arrays.asList(extensions));

        File[] pluginDirs = new File[] { new File(PLUGIN_ROOT, "plugin1") };
        when(pluginRootDir.listFiles()).thenReturn(pluginDirs);

        PluginCommandInfoSource source = new PluginCommandInfoSource(jarRootDir, pluginRootDir, parser);

        CommandInfo info = source.getCommandInfo("command-name");
        assertEquals("command-name", info.getName());

        String expectedDep1Name = new File(PLUGIN_ROOT + "/plugin1/additional-bundle").toURI().toString();
        String expectedDep2Name = new File(JAR_ROOT + "/dependency-bundle").toURI().toString();

        assertTrue(info.getDependencyResourceNames().contains(expectedDep1Name));
        assertTrue(info.getDependencyResourceNames().contains(expectedDep2Name));
    }

}
