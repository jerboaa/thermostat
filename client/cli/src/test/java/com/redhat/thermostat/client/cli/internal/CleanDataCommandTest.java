/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.client.cli.internal;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.testutils.StubBundleContext;

public class CleanDataCommandTest {

    private CleanDataCommand cleanDataCommand;
    private CommandContext mockCommandContext;
    private Storage mockStorage;
    private Console mockConsole;
    private PrintStream mockOutput;
    private AgentInfoDAO mockAgentInfoDAO;
    private Arguments mockArguments;
    private List<AgentInformation> liveAgentInfoList;
    private List<AgentInformation> allAgentInfoList;

    @Before
    public void setUp() throws DescriptorParsingException, StatementExecutionException {
        StubBundleContext bundleContext = new StubBundleContext();
        cleanDataCommand = new CleanDataCommand(bundleContext);
        
        mockStorage = mock(Storage.class); 
        bundleContext.registerService(Storage.class, mockStorage, null);
        mockAgentInfoDAO = mock(AgentInfoDAO.class);
        bundleContext.registerService(AgentInfoDAO.class, mockAgentInfoDAO, null);
        
        mockCommandContext = mock(CommandContext.class);
        mockArguments = mock(Arguments.class);
        
        when(mockCommandContext.getArguments()).thenReturn(mockArguments);
        when(mockArguments.getNonOptionArguments()).thenReturn(getValidAgentList());
        
        mockConsole = mock(Console.class);
        mockOutput = mock(PrintStream.class);
        when(mockCommandContext.getConsole()).thenReturn(mockConsole);
        when(mockConsole.getOutput()).thenReturn(mockOutput);
        
        liveAgentInfoList = new ArrayList<AgentInformation>();
        AgentInformation mockAgent1 = new AgentInformation("agentId1");
        AgentInformation mockAgent2 = new AgentInformation("agentId2");
        AgentInformation mockAgent3 = new AgentInformation("agentId3");
        AgentInformation mockAgent4 = new AgentInformation("agentId4");
        AgentInformation mockAgent5 = new AgentInformation("agentId5");
        mockAgent4.setAlive(true);
        mockAgent5.setAlive(true);
        
        liveAgentInfoList.add(mockAgent4);
        liveAgentInfoList.add(mockAgent5);
        allAgentInfoList = new ArrayList<>(5);
        allAgentInfoList.addAll(Arrays.asList(
           mockAgent1,
           mockAgent2,
           mockAgent3,
           mockAgent4,
           mockAgent5
        ));
        
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId1", "unused"))).thenReturn(mockAgent1);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId2", "unused"))).thenReturn(mockAgent2);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId3", "unused"))).thenReturn(mockAgent3);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId4", "unused"))).thenReturn(mockAgent4);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId5", "unused"))).thenReturn(mockAgent5);
    }

    @Test
    public void testOneValidArgument() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });
        when(mockArguments.getNonOptionArguments()).thenReturn(getOneValidAgent());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(1)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        
        verify(mockOutput, times(2)).println(isA(String.class));
        verify(mockOutput, times(1)).print(isA(String.class));
        verify(mockOutput).println("Purging data for agent: agentId1");
    }

    @Test
    public void testMultipleValidArguments() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });

        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(3)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockStorage).purge("agentId2");
        verify(mockStorage).purge("agentId3");
        
        verify(mockOutput, times(4)).println(isA(String.class));
        verify(mockOutput, times(1)).print(isA(String.class));
        verify(mockOutput).println("Cleaning a lot of data from Thermostat storage can cause latency for other agents or clients.");
        verify(mockOutput).print("Are you sure you want to continue (Y/y/N/n)?");
        verify(mockOutput).println("Purging data for agent: agentId1");
        verify(mockOutput).println("Purging data for agent: agentId2");
        verify(mockOutput).println("Purging data for agent: agentId3");
    }

    @Test
    public void testUnauthorizedLiveAgents() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });
        when(mockArguments.getNonOptionArguments()).thenReturn(getAliveAgents());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, never()).purge(isA(String.class));
        
        verify(mockOutput, times(3)).println(isA(String.class));
        verify(mockOutput, times(1)).print(isA(String.class));
        verify(mockOutput).println("Cleaning a lot of data from Thermostat storage can cause latency for other agents or clients.");
        verify(mockOutput).print("Are you sure you want to continue (Y/y/N/n)?");
        verify(mockOutput).println("Cannot purge data for agent agentId4. This agent is currently running");
        verify(mockOutput).println("Cannot purge data for agent agentId5. This agent is currently running");
    }

    @Test
    public void testRemoveSpecificLiveAgents() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });
        when(mockArguments.hasArgument("alive")).thenReturn(true);
        when(mockArguments.getNonOptionArguments()).thenReturn(getAliveAgents());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(2)).purge(isA(String.class));
        verify(mockStorage).purge("agentId4");
        verify(mockStorage).purge("agentId5");
        
        verify(mockOutput, times(3)).println(isA(String.class));
        verify(mockOutput, times(1)).print(isA(String.class));
        verify(mockOutput).println("Cleaning a lot of data from Thermostat storage can cause latency for other agents or clients.");
        verify(mockOutput).print("Are you sure you want to continue (Y/y/N/n)?");
        verify(mockOutput).println("Purging data for agent: agentId4");
        verify(mockOutput).println("Purging data for agent: agentId5");
    }

    @Test
    public void testInvalidArguments() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });
        when(mockArguments.getNonOptionArguments()).thenReturn(getInvalidAgentList());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, never()).purge(isA(String.class));
        verify(mockOutput, times(4)).println(isA(String.class));
        verify(mockOutput, times(1)).print(isA(String.class));
        verify(mockOutput).println("Cleaning a lot of data from Thermostat storage can cause latency for other agents or clients.");
        verify(mockOutput).print("Are you sure you want to continue (Y/y/N/n)?");
        verify(mockOutput).println("Agent with an id [invalidAgent1] was not found!");
        verify(mockOutput).println("Agent with an id [invalidAgent2] was not found!");
        verify(mockOutput).println("Agent with an id [invalidAgent3] was not found!");
    }

    @Test
    public void testRemoveAllDeadAgents() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });
        when(mockArguments.hasArgument("all")).thenReturn(true);
        when(mockAgentInfoDAO.getAllAgentInformation()).thenReturn(allAgentInfoList);
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(3)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockStorage).purge("agentId2");
        verify(mockStorage).purge("agentId3");
    }

    @Test
    public void testRemoveAllLiveAgents() throws CommandException {
        setupUserConfirmation(new byte[]{ 'Y' });
        when(mockArguments.hasArgument("all")).thenReturn(true);
        when(mockArguments.hasArgument("alive")).thenReturn(true);
        when(mockAgentInfoDAO.getAllAgentInformation()).thenReturn(allAgentInfoList);
      
        cleanDataCommand.run(mockCommandContext);
      
        verify(mockStorage, times(5)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockStorage).purge("agentId2");
        verify(mockStorage).purge("agentId3");
        verify(mockStorage).purge("agentId4");
        verify(mockStorage).purge("agentId5");
    }

    @Test
    public void testUserRefusesClean() throws CommandException {
        setupUserConfirmation(new byte[]{ 'N' });

        cleanDataCommand.run(mockCommandContext);

        verifyZeroInteractions(mockStorage);
        verify(mockOutput, times(2)).println(isA(String.class));
        verify(mockOutput, times(1)).print(isA(String.class));
        verify(mockOutput).println("Cleaning a lot of data from Thermostat storage can cause latency for other agents or clients.");
        verify(mockOutput).print("Are you sure you want to continue (Y/y/N/n)?");
        verify(mockOutput).println("Not cleaning Thermostat data at this time.");
    }

    private List<String> getValidAgentList() {
        List<String> agentIdList = new ArrayList<String>();
        agentIdList.add("agentId1");
        agentIdList.add("agentId2");
        agentIdList.add("agentId3");
        return agentIdList;
    }

    private List<String> getInvalidAgentList() {
        List<String> agentIdList = new ArrayList<String>();
        agentIdList.add("invalidAgent1");
        agentIdList.add("invalidAgent2");
        agentIdList.add("invalidAgent3");
        return agentIdList;
    }

    private List<String> getOneValidAgent() {
        List<String> agentIdList = new ArrayList<String>();
        agentIdList.add("agentId1");
        return agentIdList;
    }

    private List<String> getAliveAgents() {
        List<String> agentIdList = new ArrayList<String>();
        agentIdList.add("agentId4");
        agentIdList.add("agentId5");
        return agentIdList;
    }

    private void setupUserConfirmation(byte[] response) {
        InputStream input = new ByteArrayInputStream(response);
        when(mockConsole.getInput()).thenReturn(input);
    }
}

