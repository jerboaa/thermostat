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

package com.redhat.thermostat.client.cli.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.testutils.StubBundleContext;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AgentInformation.class)
public class CleanDataCommandTest {

    private CleanDataCommand cleanDataCommand;
    private CommandContext mockCommandContext;
    private Storage mockStorage;
    private PrintStream mockOutput;
    private AgentInfoDAO mockAgentInfoDAO;
    private Arguments mockArguments;

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
        
        Console mockConsole = mock(Console.class);
        mockOutput = mock(PrintStream.class);
        when(mockCommandContext.getConsole()).thenReturn(mockConsole);
        when(mockConsole.getOutput()).thenReturn(mockOutput);
        
        List<AgentInformation> liveAgentInfoList = new ArrayList<AgentInformation>();
        AgentInformation mockAgent1 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent2 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent3 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent4 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent5 = PowerMockito.mock(AgentInformation.class);
        when(mockAgent1.getAgentId()).thenReturn("agentId1");
        when(mockAgent2.getAgentId()).thenReturn("agentId2");
        when(mockAgent3.getAgentId()).thenReturn("agentId3");
        when(mockAgent4.getAgentId()).thenReturn("agentId4");
        when(mockAgent5.getAgentId()).thenReturn("agentId5");
        when(mockAgent4.isAlive()).thenReturn(true);
        when(mockAgent5.isAlive()).thenReturn(true);
        
        liveAgentInfoList.add(mockAgent4);
        liveAgentInfoList.add(mockAgent5);
        when(mockAgentInfoDAO.getAliveAgents()).thenReturn(liveAgentInfoList);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId1", "agentId1"))).thenReturn(mockAgent1);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId2", "agentId2"))).thenReturn(mockAgent2);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId3", "agentId3"))).thenReturn(mockAgent3);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId4", "agentId4"))).thenReturn(mockAgent4);
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId5", "agentId5"))).thenReturn(mockAgent5);
        
        Cursor<BasePojo> agentCursor = (Cursor<BasePojo>) mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(mockAgent1).thenReturn(mockAgent2).thenReturn(mockAgent3).thenReturn(mockAgent4).thenReturn(mockAgent5).thenReturn(null);
        
        PreparedStatement<BasePojo> prepared = (PreparedStatement<BasePojo>) mock(PreparedStatement.class);
        when(mockStorage.prepareStatement((StatementDescriptor<BasePojo>) any(StatementDescriptor.class))).thenReturn(prepared);
        when(prepared.executeQuery()).thenReturn(agentCursor);
    }

    @Test
    public void testOneValidArgument() throws CommandException {
        when(mockArguments.getNonOptionArguments()).thenReturn(getOneValidAgent());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(1)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        
        verify(mockOutput, times(1)).println(isA(String.class));
        verify(mockOutput).println("Purging data for agent: agentId1");
    }

    @Test
    public void testMultipleValidArguments() throws CommandException {
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(3)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockStorage).purge("agentId2");
        verify(mockStorage).purge("agentId3");
        
        verify(mockOutput, times(3)).println(isA(String.class));
        verify(mockOutput).println("Purging data for agent: agentId1");
        verify(mockOutput).println("Purging data for agent: agentId2");
        verify(mockOutput).println("Purging data for agent: agentId3");
    }

    @Test
    public void testUnauthorizedLiveAgents() throws CommandException {
        when(mockArguments.getNonOptionArguments()).thenReturn(getAliveAgents());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, never()).purge(isA(String.class));
        
        verify(mockOutput, times(2)).println(isA(String.class));
        verify(mockOutput).println("Cannot purge data for agent agentId4. This agent is currently running");
        verify(mockOutput).println("Cannot purge data for agent agentId5. This agent is currently running");
    }

    @Test
    public void testRemoveSpecificLiveAgents() throws CommandException {
        when(mockArguments.hasArgument("alive")).thenReturn(true);
        when(mockArguments.getNonOptionArguments()).thenReturn(getAliveAgents());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(2)).purge(isA(String.class));
        verify(mockStorage).purge("agentId4");
        verify(mockStorage).purge("agentId5");
        
        verify(mockOutput, times(2)).println(isA(String.class));
        verify(mockOutput).println("Purging data for agent: agentId4");
        verify(mockOutput).println("Purging data for agent: agentId5");
    }

    @Test
    public void testInvalidArguments() throws CommandException {
        when(mockArguments.getNonOptionArguments()).thenReturn(getInvalidAgentList());
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, never()).purge(isA(String.class));
        verify(mockOutput, times(3)).println(isA(String.class));
        verify(mockOutput).println("Agent with an id [invalidAgent1] was not found!");
        verify(mockOutput).println("Agent with an id [invalidAgent2] was not found!");
        verify(mockOutput).println("Agent with an id [invalidAgent3] was not found!");
    }

    @Test
    public void testRemoveAllDeadAgents() throws CommandException {
        when(mockArguments.hasArgument("all")).thenReturn(true);
        
        cleanDataCommand.run(mockCommandContext);
        
        verify(mockStorage, times(3)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockStorage).purge("agentId2");
        verify(mockStorage).purge("agentId3");
    }

    @Test
    public void testRemoveAllLiveAgents() throws CommandException {
        when(mockArguments.hasArgument("all")).thenReturn(true);
        when(mockArguments.hasArgument("alive")).thenReturn(true);
      
        cleanDataCommand.run(mockCommandContext);
      
        verify(mockStorage, times(5)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockStorage).purge("agentId2");
        verify(mockStorage).purge("agentId3");
        verify(mockStorage).purge("agentId4");
        verify(mockStorage).purge("agentId5");
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

}
