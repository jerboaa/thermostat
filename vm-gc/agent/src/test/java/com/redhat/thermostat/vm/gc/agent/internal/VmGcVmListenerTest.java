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

package com.redhat.thermostat.vm.gc.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;

import com.redhat.thermostat.storage.model.VmGcStat;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;

public class VmGcVmListenerTest {
    private static final String[] GC_NAMES = new String[] { "GC1", "GC2" };
    private static final Long[] GC_INVOCS = new Long[] { 500L, 1000L };
    private static final Long[] GC_TIMES = new Long[] { 5000L, 10000L };
    
    private VmGcVmListener vmListener;
    private MonitoredVm monitoredVm;
    private VmGcDataExtractor extractor;
    private VmGcStatDAO vmGcStatDAO;
    
    @Before
    public void setup() throws MonitorException {
        final int numGCs = 2;
        vmGcStatDAO = mock(VmGcStatDAO.class);
        vmListener = new VmGcVmListener(vmGcStatDAO, 0);
        
        monitoredVm = mock(MonitoredVm.class);
        extractor = mock(VmGcDataExtractor.class);
        
        for (int i = 0; i < numGCs; i++) {
            mockCollectorName(i);
            mockCollectorInvocations(i);
            mockCollectorTime(i);
        }
        
        when(extractor.getTotalCollectors()).thenReturn((long) GC_NAMES.length);
    }

    private void mockCollectorName(int gc) throws MonitorException {
        when(extractor.getCollectorName(gc)).thenReturn(GC_NAMES[gc]);
    }
    
    private void mockCollectorInvocations(int gc) throws MonitorException {
        when(extractor.getCollectorInvocations(gc)).thenReturn(GC_INVOCS[gc]);
    }

    private void mockCollectorTime(int gc) throws MonitorException {
        when(extractor.getCollectorTime(gc)).thenReturn(GC_TIMES[gc]);
    }
    
    @Test
    public void testRecordMemoryStat() {
        final int numCollectors = GC_NAMES.length;
        vmListener.recordGcStat(monitoredVm, extractor);
        ArgumentCaptor<VmGcStat> captor = ArgumentCaptor.forClass(VmGcStat.class);
        verify(vmGcStatDAO, times(numCollectors)).putVmGcStat(captor.capture());
        List<VmGcStat> gcStats = captor.getAllValues();
        
        for (int i = 0; i < numCollectors; i++) {
            VmGcStat stat = gcStats.get(i);
            assertEquals(GC_NAMES[i], stat.getCollectorName());
            assertEquals(GC_INVOCS[i], (Long) stat.getRunCount());
            assertEquals(GC_TIMES[i], (Long) stat.getWallTime());
        }
    }
}

