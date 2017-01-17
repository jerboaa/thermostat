/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.populator.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.populator.internal.AgentInfoPopulator;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.dependencies.ProcessedRecords;
import com.redhat.thermostat.storage.populator.internal.dependencies.SharedState;

public class AgentInfoPopulatorTest {

    @Test
    public void canHandleAgentInfoCollection() {
        AgentInfoPopulator putter = new AgentInfoPopulator();
        assertEquals("agent-config", putter.getHandledCollection());
    }
    
    @Test
    public void canAddPojos() {
        int totalRecords = 23;
        AgentInfoDAO dao = mock(AgentInfoDAO.class);
        // Initially return 0, then return the expected count
        when(dao.getCount()).thenReturn(0L).thenReturn((long)totalRecords);
        AgentInfoPopulator putter = new AgentInfoPopulator(dao);
        ConfigItem item = new ConfigItem(totalRecords, 3, "agent-config");
        SharedState state = new SharedState();
        Console console = mock(Console.class);
        PrintStream ps = mock(PrintStream.class);
        when(console.getOutput()).thenReturn(ps);
        state = putter.addPojos(item, state, console);
        ArgumentCaptor<AgentInformation> agentInfoCaptor = ArgumentCaptor.forClass(AgentInformation.class);
        verify(dao, times(23)).addAgentInformation(agentInfoCaptor.capture());
        List<AgentInformation> agentInfos = agentInfoCaptor.getAllValues();
        List<AgentInformation> filtered = getAliveAgentInfos(agentInfos);
        assertEquals(3, filtered.size());
        ProcessedRecords<String> recs = state.getProcessedRecordsFor("agentId");
        assertEquals(23, recs.getAll().size());
        ProcessedRecords<String> notExisting = state.getProcessedRecordsFor("foo-bar");
        assertNull(notExisting);
    }

    private List<AgentInformation> getAliveAgentInfos(List<AgentInformation> agentInfos) {
        List<AgentInformation> aliveInfos = new ArrayList<>();
        for (AgentInformation info: agentInfos) {
            if (info.isAlive()) {
                aliveInfos.add(info);
            }
        }
        return aliveInfos;
    }
}
