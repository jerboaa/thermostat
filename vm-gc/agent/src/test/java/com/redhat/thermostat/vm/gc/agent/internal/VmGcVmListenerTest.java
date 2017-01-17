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

package com.redhat.thermostat.vm.gc.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;

public class VmGcVmListenerTest {

    private static final long OS_TICKS_PER_SECOND = 1_000_000;

    private static final String[] GC_NAMES = new String[] { "GC1", "GC2" };
    private static final Long[] GC_INVOCS = new Long[] { 500L, 1000L };
    private static final Long[] GC_TIMES = new Long[] { 5000L, 10000L };
    
    private VmGcVmListener vmListener;
    private VmGcDataExtractor extractor;
    private VmGcStatDAO vmGcStatDAO;
    
    @Before
    public void setup() throws VmUpdateException {
        final int numGCs = 2;
        vmGcStatDAO = mock(VmGcStatDAO.class);
        vmListener = new VmGcVmListener("foo-agent", vmGcStatDAO, "vmId");
        
        extractor = mock(VmGcDataExtractor.class);
        
        for (int i = 0; i < numGCs; i++) {
            mockCollectorName(i);
            mockCollectorInvocations(i);
            mockCollectorTime(i);
        }
        
        when(extractor.getFrequency()).thenReturn(OS_TICKS_PER_SECOND);
        when(extractor.getTotalCollectors()).thenReturn((long) GC_NAMES.length);
    }

    private void mockCollectorName(int gc) throws VmUpdateException {
        when(extractor.getCollectorName(gc)).thenReturn(GC_NAMES[gc]);
    }
    
    private void mockCollectorInvocations(int gc) throws VmUpdateException {
        when(extractor.getCollectorInvocations(gc)).thenReturn(GC_INVOCS[gc]);
    }

    private void mockCollectorTime(int gc) throws VmUpdateException {
        when(extractor.getCollectorTime(gc)).thenReturn(GC_TIMES[gc]);
    }
    
    @Test
    public void testRecordMemoryStat() {
        final int numCollectors = GC_NAMES.length;
        vmListener.recordGcStat(extractor);
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
    
    @Test
    public void testRecordMemoryStatNoName() throws VmUpdateException {
        when(extractor.getCollectorName(1)).thenReturn(null);
        vmListener.recordGcStat(extractor);
        ArgumentCaptor<VmGcStat> captor = ArgumentCaptor.forClass(VmGcStat.class);
        verify(vmGcStatDAO).putVmGcStat(captor.capture());
        List<VmGcStat> gcStats = captor.getAllValues();
        
        // Verify second collector skipped
        assertEquals(1, gcStats.size());
        
        VmGcStat stat = gcStats.get(0);
        assertEquals(GC_NAMES[0], stat.getCollectorName());
        assertEquals(GC_INVOCS[0], (Long) stat.getRunCount());
        assertEquals(GC_TIMES[0], (Long) stat.getWallTime());
    }
    
    @Test
    public void testRecordMemoryStatNoInvocations() throws VmUpdateException {
        when(extractor.getCollectorInvocations(1)).thenReturn(null);
        vmListener.recordGcStat(extractor);
        ArgumentCaptor<VmGcStat> captor = ArgumentCaptor.forClass(VmGcStat.class);
        verify(vmGcStatDAO).putVmGcStat(captor.capture());
        List<VmGcStat> gcStats = captor.getAllValues();
        
        // Verify second collector skipped
        assertEquals(1, gcStats.size());
        
        VmGcStat stat = gcStats.get(0);
        assertEquals(GC_NAMES[0], stat.getCollectorName());
        assertEquals(GC_INVOCS[0], (Long) stat.getRunCount());
        assertEquals(GC_TIMES[0], (Long) stat.getWallTime());
    }
    
    @Test
    public void testRecordMemoryStatNoTime() throws VmUpdateException {
        when(extractor.getCollectorTime(1)).thenReturn(null);
        vmListener.recordGcStat(extractor);
        ArgumentCaptor<VmGcStat> captor = ArgumentCaptor.forClass(VmGcStat.class);
        verify(vmGcStatDAO).putVmGcStat(captor.capture());
        List<VmGcStat> gcStats = captor.getAllValues();
        
        // Verify second collector skipped
        assertEquals(1, gcStats.size());
        
        VmGcStat stat = gcStats.get(0);
        assertEquals(GC_NAMES[0], stat.getCollectorName());
        assertEquals(GC_INVOCS[0], (Long) stat.getRunCount());
        assertEquals(GC_TIMES[0], (Long) stat.getWallTime());
    }
    
    @Test
    public void testRecordMemoryStatNoTotal() throws VmUpdateException {
        when(extractor.getTotalCollectors()).thenReturn(null);
        vmListener.recordGcStat(extractor);
        verify(vmGcStatDAO, never()).putVmGcStat(any(VmGcStat.class));
    }

    @Test
    public void testRecordMemoryFrequencyMismatch() throws VmUpdateException {
        final long SOME_FREQUENCY = 100l;
        when(extractor.getFrequency()).thenReturn(SOME_FREQUENCY);
        vmListener.recordGcStat(extractor);
        ArgumentCaptor<VmGcStat> captor = ArgumentCaptor.forClass(VmGcStat.class);
        verify(vmGcStatDAO, times(2)).putVmGcStat(captor.capture());
        List<VmGcStat> gcStats = captor.getAllValues();

        VmGcStat stat = gcStats.get(0);
        assertEquals(GC_NAMES[0], stat.getCollectorName());
        assertEquals(GC_INVOCS[0], (Long) stat.getRunCount());
        assertEquals(TimeUnit.SECONDS.toMicros(GC_TIMES[0]/SOME_FREQUENCY), stat.getWallTime());
    }

}

