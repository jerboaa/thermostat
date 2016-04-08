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

package com.redhat.thermostat.dev.populator.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.ProcessedRecords;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;

public class HostInfoPopulatorTest {

    @Test
    public void handlesCorrectCollection() {
        HostInfoPopulator populator = new HostInfoPopulator();
        assertEquals("host-info", populator.getHandledCollection());
    }
    
    @Test
    public void canPopulateToStorage() {
        String[] agents = new String[] {
                "host-agent1", "host-agent2", "host-agent3", "host-agent4"
        };
        List<String> agentIds = Arrays.asList(agents);
        SharedState state = mock(SharedState.class);
        @SuppressWarnings("unchecked")
        ProcessedRecords<String> procRecs = mock(ProcessedRecords.class);
        when(procRecs.getAll()).thenReturn(agentIds);
        when(state.getProcessedRecordsFor(eq("agentId"))).thenReturn(procRecs);
        when(state.getProperty(eq("agent-info-dao"))).thenReturn(mock(AgentInfoDAO.class));
        int totalRecords = agents.length;
        HostInfoDAO dao = mock(HostInfoDAO.class);
        // Initially return 0, then return the expected count
        when(dao.getCount()).thenReturn(0L).thenReturn((long)totalRecords);
        
        ConfigItem config = new ConfigItem(1, ConfigItem.UNSET, "host-info");
        HostInfoPopulator populator = new HostInfoPopulator(dao);
        populator.addPojos(mock(Storage.class), config, state);
        ArgumentCaptor<HostInfo> captor = ArgumentCaptor.forClass(HostInfo.class);
        verify(dao, times(totalRecords)).putHostInfo(captor.capture());
        List<HostInfo> values = captor.getAllValues();
        // ensure no memory values are negative
        for (HostInfo info: values) {
            if (info.getTotalMemory() < 0) {
                throw new AssertionError("Invalid memory value: " + info.getTotalMemory() + " < 0");
            }
        }
    }
}
