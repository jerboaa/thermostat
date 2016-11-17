/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.storage.populator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.TabCompleter;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class StoragePopulatorCommandTest {

    private static final String SOME_CONFIG = "config";

    private StoragePopulatorCommand command;
    private CommandContext ctx;
    private Arguments args;

    CommonPaths paths;
    HostInfoDAO hostInfoDAO;
    AgentInfoDAO agentInfoDAO;
    VmInfoDAO vmInfoDAO;
    NetworkInterfaceInfoDAO networkInfoDAO;
    ThreadDao threadDao;

    private ByteArrayOutputStream outputBAOS, errorBAOS;
    private PrintStream output, error;
    private Console console;

    @Before
    public void setUp() {
        ctx = mock(CommandContext.class);
        args = mock(Arguments.class);

        console = mock(Console.class);

        outputBAOS = new ByteArrayOutputStream();
        output = new PrintStream(outputBAOS);

        errorBAOS = new ByteArrayOutputStream();
        error = new PrintStream(errorBAOS);

        when(ctx.getArguments()).thenReturn(args);
        when(ctx.getConsole()).thenReturn(console);
        when(console.getError()).thenReturn(error);
        when(console.getOutput()).thenReturn(output);
    }

    @Test
    public void testCommandFailsWhenDependenciesUnavailable() {
        command = new StoragePopulatorCommand();

        try {
            command.run(ctx);
            fail("A CommandException was expected but not thrown.");
        } catch (CommandException e) {
            assertTrue(e.getMessage().matches("Required service .* is unavailable"));
        }
    }

    @Test
    public void testCommandFailsWithNonexistentConfig() throws CommandException {
        final File nonexistentConfig = mock(File.class);
        when(nonexistentConfig.exists()).thenReturn(false);
        when(nonexistentConfig.getAbsolutePath()).thenReturn("/foo/bar/" + SOME_CONFIG);
        command = new StoragePopulatorCommand() {
            @Override
            File getConfigFile(Arguments args) {
                return nonexistentConfig;
            }
        };
        setUpServices();

        command.run(ctx);
        String errorString = new String(errorBAOS.toByteArray());
        assertEquals("Config file \"/foo/bar/config\" does not exist!\n", errorString);
    }

    @Test
    public void testCommandFailsWithInvalidConfig() throws CommandException, IOException {
        final File invalidConfig = mock(File.class);
        when(invalidConfig.exists()).thenReturn(true);
        when(invalidConfig.toPath()).thenThrow(IOException.class);

        command = new StoragePopulatorCommand() {
            @Override
            File getConfigFile(Arguments args) {
                return invalidConfig;
            }
        };
        setUpServices();

        command.run(ctx);
        String errorString = new String(errorBAOS.toByteArray());
        assertEquals("Failed to parse config file.\n", errorString);
    }

    @Test
    public void testCommandFailsWithInvalidPopulator() throws CommandException {
        final File invalidConfigFile = new File(
                getClass().getResource("/invalid-config.json").getFile());
        command = new StoragePopulatorCommand() {
            @Override
            File getConfigFile(Arguments args) {
                return invalidConfigFile;
            }
        };
        setUpServices();

        command.run(ctx);
        String errorString = new String(errorBAOS.toByteArray());
        assertEquals("No populator for collection \"foo\" found.\n", errorString);
    }

    @Test
    public void testCommandPopulatesDatabase() throws CommandException {
        final File validConfigFile = new File(
                getClass().getResource("/valid-config.json").getFile());
        command = new StoragePopulatorCommand() {
            @Override
            File getConfigFile(Arguments args) {
                return validConfigFile;
            }
        };

        setUpServices();

        // the following counts are dependent on the parameters in valid-config.json
        int agentCount = 2;
        int hostCount = 2;
        int networkCount = 40;
        int deadLockCount = 100;
        int vmCount = 10;

        when(agentInfoDAO.getCount()).thenReturn(0l).thenReturn((long) agentCount);
        when(hostInfoDAO.getCount()).thenReturn(0l).thenReturn((long) hostCount);
        when(networkInfoDAO.getCount()).thenReturn(0l).thenReturn((long) networkCount);
        when(threadDao.getDeadLockCount()).thenReturn(0l).thenReturn((long) deadLockCount);
        when(vmInfoDAO.getCount()).thenReturn(0l).thenReturn((long) vmCount);

        command.run(ctx);

        verify(agentInfoDAO, times(agentCount)).addAgentInformation(any(AgentInformation.class));
        verify(hostInfoDAO, times(hostCount)).putHostInfo(any(HostInfo.class));
        verify(networkInfoDAO, times(networkCount)).putNetworkInterfaceInfo(
                any(NetworkInterfaceInfo.class));
        verify(threadDao, times(deadLockCount)).saveDeadLockStatus(any(VmDeadLockData.class));
        verify(vmInfoDAO, times(vmCount)).putVmInfo(any(VmInfo.class));
    }

    @Test
    public void testProvidesConfigFileCompletions() {
        command = new StoragePopulatorCommand();
        setUpServices();
        Map<CliCommandOption, ? extends TabCompleter> map = command.getOptionCompleters();
        assertThat(map.keySet(), is(equalTo(Collections.singleton(StoragePopulatorCommand.CONFIG_OPTION))));
        assertThat(map.get(StoragePopulatorCommand.CONFIG_OPTION), is(not(equalTo(null))));
    }

    @Test
    public void testProvidesTabCompletionsEvenWhenCommonPathsUnavailable() {
        command = new StoragePopulatorCommand();
        setUpServices();
        command.unbindPaths(mock(CommonPaths.class));
        Map<CliCommandOption, ? extends TabCompleter> map = command.getOptionCompleters();
        assertThat(map.keySet(), is(equalTo(Collections.singleton(StoragePopulatorCommand.CONFIG_OPTION))));
        assertThat(map.get(StoragePopulatorCommand.CONFIG_OPTION), is(not(equalTo(null))));
    }

    private void setUpServices () {
        paths = mock(CommonPaths.class);
        command.bindPaths(paths);
        hostInfoDAO = mock(HostInfoDAO.class);
        command.bindHostInfoDAO(hostInfoDAO);
        agentInfoDAO = mock(AgentInfoDAO.class);
        command.bindAgentInfoDAO(agentInfoDAO);
        vmInfoDAO = mock(VmInfoDAO.class);
        command.bindVmInfoDAO(vmInfoDAO);
        networkInfoDAO = mock(NetworkInterfaceInfoDAO.class);
        command.bindNetworkInfoDAO(networkInfoDAO);
        threadDao = mock(ThreadDao.class);
        command.bindThreadDAO(threadDao);
    }
}
