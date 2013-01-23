/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.VmEvent;

import com.redhat.thermostat.storage.model.VmMemoryStat;
import com.redhat.thermostat.storage.model.VmMemoryStat.Generation;
import com.redhat.thermostat.storage.model.VmMemoryStat.Space;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class VmMemoryVmListenerTest {
    private static final String[] GEN_NAMES = new String[] { "Gen1", "Gen2" };
    private static final Long[] GEN_CAPS = new Long[] { 500L, 1000L };
    private static final Long[] GEN_MAX_CAPS = new Long[] { 5000L, 10000L };
    private static final String[] GEN_GCS = new String[] { "GC1", "GC2" };
    private static final Long[] GEN_SPACES = new Long[] { 2L, 1L };
    private static final String[][] SPACE_NAME = new String[][] { 
        { "Space1", "Space2" },
        { "Space3" }
    };
    private static final Long[][] SPACE_CAPS = new Long[][] {
        { 225L, 275L },
        { 1000L }
    };
    private static final Long[][] SPACE_MAX_CAPS = new Long[][] {
        { 2250L, 2750L },
        { 10000L }
    };
    private static final Long[][] SPACE_USED = new Long[][] {
        { 125L, 175L },
        { 900L }
    };
    
    private VmMemoryVmListener vmListener;
    private VmMemoryDataExtractor extractor;
    private VmMemoryStatDAO vmMemoryStatDAO;
    private MonitoredVm monitoredVm;
    
    @Before
    public void setup() throws MonitorException {
        final int numGens = 2;
        vmMemoryStatDAO = mock(VmMemoryStatDAO.class);
        vmListener = new VmMemoryVmListener(vmMemoryStatDAO, 0);
        monitoredVm = mock(MonitoredVm.class);
        extractor = mock(VmMemoryDataExtractor.class);

        mockTotalGenerations(numGens);

        for (int i = 0; i < numGens; i++) {
            mockGenerationName(i);
            mockGenerationCapacity(i);
            mockGenerationMaxCapacity(i);
            mockGenerationGC(i);
            mockTotalSpaces(i);
            int numSpaces = GEN_SPACES[i].intValue();
            for (int j = 0; j < numSpaces; j++) {
                mockSpaceName(i, j);
                mockSpaceCapacity(i, j);
                mockSpaceMaxCapacity(i, j);
                mockSpaceUsed(i, j);
            }
        }
    }

    private void mockTotalGenerations(long gens) throws MonitorException {
        when(extractor.getTotalGcGenerations()).thenReturn(gens);
    }

    private void mockGenerationName(int gen) throws MonitorException {
        when(extractor.getGenerationName(gen)).thenReturn(GEN_NAMES[gen]);
    }
    
    private void mockGenerationCapacity(int gen) throws MonitorException {
        when(extractor.getGenerationCapacity(gen)).thenReturn(GEN_CAPS[gen]);
    }

    private void mockGenerationMaxCapacity(int gen) throws MonitorException {
        when(extractor.getGenerationMaxCapacity(gen)).thenReturn(GEN_MAX_CAPS[gen]);
    }
    
    private void mockGenerationGC(int gen) throws MonitorException {
        when(extractor.getGenerationCollector(gen)).thenReturn(GEN_GCS[gen]);
    }
    
    private void mockTotalSpaces(int gen) throws MonitorException {
        when(extractor.getTotalSpaces(gen)).thenReturn(GEN_SPACES[gen]);
    }
    
    private void mockSpaceName(int gen, int space) throws MonitorException {
        when(extractor.getSpaceName(gen, space)).thenReturn(SPACE_NAME[gen][space]);
    }
    
    private void mockSpaceCapacity(int gen, int space) throws MonitorException {
        when(extractor.getSpaceCapacity(gen, space)).thenReturn(SPACE_CAPS[gen][space]);
    }
    
    private void mockSpaceMaxCapacity(int gen, int space) throws MonitorException {
        when(extractor.getSpaceMaxCapacity(gen, space)).thenReturn(SPACE_MAX_CAPS[gen][space]);
    }
    
    private void mockSpaceUsed(int gen, int space) throws MonitorException {
        when(extractor.getSpaceUsed(gen, space)).thenReturn(SPACE_USED[gen][space]);
    }

    @Test
    public void testDisconnectedIsNoOp() {
        vmListener.disconnected(null);

        verifyNoMoreInteractions(vmMemoryStatDAO, extractor);
    }

    @Test
    public void testMonitorStatusChangeIsNoOp() {
        vmListener.monitorStatusChanged(null);

        verifyNoMoreInteractions(vmMemoryStatDAO, extractor);
    }

    @Test
    public void testMonitorsUpdated() throws MonitorException {
        Monitor monitor = mock(Monitor.class);
        when(monitor.getValue()).thenReturn(Long.valueOf(0));
        when(monitoredVm.findByName(anyString())).thenReturn(monitor);
        VmEvent monitorUpdateEvent = mock(VmEvent.class);
        when(monitorUpdateEvent.getMonitoredVm()).thenReturn(monitoredVm);

        vmListener.monitorsUpdated(monitorUpdateEvent);

        verify(vmMemoryStatDAO).putVmMemoryStat(isA(VmMemoryStat.class));
    }

    @Test
    public void testRecordMemoryStat() {
        vmListener.recordMemoryStat(extractor);
        ArgumentCaptor<VmMemoryStat> captor = ArgumentCaptor.forClass(VmMemoryStat.class);
        verify(vmMemoryStatDAO).putVmMemoryStat(captor.capture());
        VmMemoryStat memoryStat = captor.getValue();
        
        Generation[] gens = memoryStat.getGenerations();
        assertEquals(2, gens.length);
        for (int i = 0; i < gens.length; i++) {
            Generation gen = gens[i];
            assertEquals(GEN_NAMES[i], gen.getName());
            assertEquals(GEN_CAPS[i], (Long) gen.getCapacity());
            assertEquals(GEN_MAX_CAPS[i], (Long) gen.getMaxCapacity());
            assertEquals(GEN_GCS[i], gen.getCollector());
            assertEquals(GEN_SPACES[i], Long.valueOf(gen.getSpaces().length));
            Space[] spaces = gen.getSpaces();
            for (int j = 0; j < spaces.length; j++) {
                Space space = spaces[j];
                assertEquals(SPACE_NAME[i][j], space.getName());
                assertEquals(SPACE_CAPS[i][j], (Long) space.getCapacity());
                assertEquals(SPACE_MAX_CAPS[i][j], (Long) space.getMaxCapacity());
                assertEquals(SPACE_USED[i][j], (Long) space.getUsed());
            }
        }
    }

    @Test
    public void testRecordingMemoryInPresenseOfExtrationErrors() throws MonitorException {
        when(extractor.getTotalGcGenerations()).thenThrow(new MonitorException());
        vmListener.recordMemoryStat(extractor);

        verifyNoMoreInteractions(vmMemoryStatDAO);
    }
}

