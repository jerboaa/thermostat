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
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.testutils.StubBundleContext;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AgentInformation.class)
public class CleanHistoryCommandTest {

    private CleanHistoryCommand cleanHistoryCommand;
    private CommandContext mockCommandContext;
    private List<String> agentIdList;
    private Storage mockStorage;
    private PrintStream mockOutput;
    private AgentInfoDAO mockAgentInfoDAO;

    @Before
    public void setUp() {
        StubBundleContext bundleContext = new StubBundleContext();
        cleanHistoryCommand = new CleanHistoryCommand(bundleContext);
        
        mockStorage = mock(Storage.class); 
        bundleContext.registerService(Storage.class, mockStorage, null);
        mockAgentInfoDAO = mock(AgentInfoDAO.class);
        bundleContext.registerService(AgentInfoDAO.class, mockAgentInfoDAO, null);
        
        mockCommandContext = mock(CommandContext.class);
        Arguments mockArguments = mock(Arguments.class);
        
        when(mockCommandContext.getArguments()).thenReturn(mockArguments);
        when(mockArguments.getNonOptionArguments()).thenReturn(getArgumentList());
        
        Console mockConsole = mock(Console.class);
        mockOutput = mock(PrintStream.class);
        when(mockCommandContext.getConsole()).thenReturn(mockConsole);
        when(mockConsole.getOutput()).thenReturn(mockOutput);
    }

    @Test
    public void testOneValidArgument() throws CommandException {
        AgentInformation mockAgent = PowerMockito.mock(AgentInformation.class);
        when(mockAgent.getAgentId()).thenReturn("agentId1");
        when(mockAgent.isAlive()).thenReturn(false);
        
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId1", "agentId1"))).thenReturn((mockAgent));
        
        cleanHistoryCommand.run(mockCommandContext);
        
        verify(mockStorage, times(1)).purge(isA(String.class));
        verify(mockStorage).purge("agentId1");
        verify(mockOutput).println("Purging data for agent: agentId1");
    }

    @Test
    public void testMultipleValidArguments() throws CommandException {
        AgentInformation mockAgent1 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent2 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent3 = PowerMockito.mock(AgentInformation.class);
        when(mockAgent1.getAgentId()).thenReturn("agentId1");
        when(mockAgent2.getAgentId()).thenReturn("agentId2");
        when(mockAgent3.getAgentId()).thenReturn("agentId3");
        
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId1", "agentId1"))).thenReturn((mockAgent1));
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId2", "agentId2"))).thenReturn((mockAgent2));
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId3", "agentId3"))).thenReturn((mockAgent3));
        
        cleanHistoryCommand.run(mockCommandContext);
        
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
    public void testLiveAgents() throws CommandException {
        AgentInformation mockAgent1 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent2 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent3 = PowerMockito.mock(AgentInformation.class);
        when(mockAgent1.getAgentId()).thenReturn("agentId1");
        when(mockAgent2.getAgentId()).thenReturn("agentId2");
        when(mockAgent3.getAgentId()).thenReturn("agentId3");
        when(mockAgent1.isAlive()).thenReturn(true);
        when(mockAgent2.isAlive()).thenReturn(false);
        when(mockAgent3.isAlive()).thenReturn(true);
        
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId1", "agentId1"))).thenReturn((mockAgent1));
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId2", "agentId2"))).thenReturn((mockAgent2));
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("agentId3", "agentId3"))).thenReturn((mockAgent3));
        
        cleanHistoryCommand.run(mockCommandContext);
        
        verify(mockStorage, times(1)).purge(isA(String.class));
        verify(mockStorage, times(1)).purge("agentId2");
        
        verify(mockOutput, times(3)).println(isA(String.class));
        verify(mockOutput).println("Cannot purge data for agent agentId1. This agent is currently running");
        verify(mockOutput).println("Purging data for agent: agentId2");
        verify(mockOutput).println("Cannot purge data for agent agentId3. This agent is currently running");
    }

    @Test
    public void testInvalidArguments() throws CommandException {
        AgentInformation mockAgent1 = PowerMockito.mock(AgentInformation.class);
        AgentInformation mockAgent2 = PowerMockito.mock(AgentInformation.class);
        when(mockAgent1.getAgentId()).thenReturn("validAgent1-in-DB");
        when(mockAgent2.getAgentId()).thenReturn("validAgent2-in-DB");
        
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("validAgent1-in-DB", "validAgent1-in-DB"))).thenReturn((mockAgent1));
        when(mockAgentInfoDAO.getAgentInformation(new HostRef("validAgent2-in-DB", "validAgent2-in-DB"))).thenReturn((mockAgent2));
        
        cleanHistoryCommand.run(mockCommandContext);
        
        verify(mockStorage, never()).purge(isA(String.class));
        
        verify(mockOutput, times(3)).println(isA(String.class));
        verify(mockOutput).println("Agent with an id agentId1 was not found!");
        verify(mockOutput).println("Agent with an id agentId2 was not found!");
        verify(mockOutput).println("Agent with an id agentId3 was not found!");
    }

    private List<String> getArgumentList() {
        agentIdList = new ArrayList<String>();
        agentIdList.add("agentId1");
        agentIdList.add("agentId2");
        agentIdList.add("agentId3");
        return agentIdList;
    }

}
