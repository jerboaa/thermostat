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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.ProcessedRecords;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class VmInfoPopulatorTest {

    @Test
    public void canHandleCorrectCollection() {
        VmInfoPopulator populator = new VmInfoPopulator();
        assertEquals("vm-info", populator.getHandledCollection());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void canPopulateData() {
        VmInfoDAO mockDAO = mock(VmInfoDAO.class);
        SharedState state = mock(SharedState.class);
        ProcessedRecords<String> procRecs = mock(ProcessedRecords.class);
        String[] agents = new String[] {
                "foo-agent1", "foo-agent2", "bar-agent3"
        };
        List<String> agentIdList = Arrays.asList(agents);
        when(procRecs.getAll()).thenReturn(agentIdList);
        when(state.getProcessedRecordsFor(eq("agentId"))).thenReturn(procRecs);
        int perAgentVms = 44;
        int subsetAlive = 23;
        ConfigItem config = new ConfigItem(perAgentVms, subsetAlive, "vm-info");
        
        // total records inserted will be 3 * 44. 44 records per agentId
        int totalRecords = perAgentVms * agents.length;
        // Initially return 0, then return the expected count
        when(mockDAO.getCount()).thenReturn(0L).thenReturn((long)totalRecords);
        VmInfoPopulator populator = new VmInfoPopulator(mockDAO);
        populator.addPojos(mock(Storage.class), config, state);
        ArgumentCaptor<VmInfo> captor = ArgumentCaptor.forClass(VmInfo.class);
        verify(mockDAO, times(totalRecords)).putVmInfo(captor.capture());
        List<VmInfo> allInfos = captor.getAllValues();
        assertEquals(totalRecords, allInfos.size());
        List<VmInfo> aliveInfos = getAliveVmInfos(allInfos);
        int totalAliveRecs = subsetAlive * agents.length;
        assertEquals(totalAliveRecs, aliveInfos.size());
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ProcessedRecords> procsVms = ArgumentCaptor.forClass(ProcessedRecords.class);
        verify(state).addProcessedRecords(eq("vmId"), procsVms.capture());
        ProcessedRecords<String> processedVms = procsVms.getValue();
        assertEquals("Excected vmIds added to shared state", totalRecords, processedVms.getAll().size());
    }
    
    @SuppressWarnings("deprecation")
    private List<VmInfo> getAliveVmInfos(List<VmInfo> allInfos) {
        List<VmInfo> aliveInfos = new ArrayList<>();
        for (VmInfo info: allInfos) {
            if (info.isAlive()) {
                aliveInfos.add(info);
            }
        }
        return aliveInfos;
    }
}
