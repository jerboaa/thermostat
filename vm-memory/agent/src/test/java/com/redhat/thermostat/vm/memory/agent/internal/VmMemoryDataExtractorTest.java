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

package com.redhat.thermostat.vm.memory.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.redhat.thermostat.storage.model.VmMemoryStat.Generation;

import sun.jvmstat.monitor.LongMonitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.StringMonitor;

public class VmMemoryDataExtractorTest {

    private MonitoredVm buildStringMonitoredVm(String monitorName, String monitorReturn) throws MonitorException {
        final StringMonitor monitor = mock(StringMonitor.class);
        when(monitor.stringValue()).thenReturn(monitorReturn);
        when(monitor.getValue()).thenReturn(monitorReturn);
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName(monitorName)).thenReturn(monitor);
        return vm;
    }

    private MonitoredVm buildLongMonitoredVm(String monitorName, Long monitorReturn) throws MonitorException {
        final LongMonitor monitor = mock(LongMonitor.class);
        when(monitor.longValue()).thenReturn(monitorReturn);
        when(monitor.getValue()).thenReturn(monitorReturn);
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName(monitorName)).thenReturn(monitor);
        return vm;
    }

    @Test
    public void testTotalGcGenerations() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.policy.generations";
        final Long GC_GENERATIONS = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, GC_GENERATIONS);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getTotalGcGenerations();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(GC_GENERATIONS, returned);
    }

    @Test
    public void testGenerationName() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.name";
        final String GENERATION_NAME = "Youth";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, GENERATION_NAME);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        String returned = extractor.getGenerationName(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(GENERATION_NAME, returned);
    }

    @Test
    public void testGenerationCapacity() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.capacity";
        final Long GENERATION_CAPACITY = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, GENERATION_CAPACITY);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getGenerationCapacity(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(GENERATION_CAPACITY, returned);
    }

    @Test
    public void testGenerationMaxCapacity() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.maxCapacity";
        final Long GENERATION_MAX_CAPACITY = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, GENERATION_MAX_CAPACITY);


        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getGenerationMaxCapacity(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(GENERATION_MAX_CAPACITY, returned);
    }

    @Test
    public void testGenerationCollector() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.collector.0.name";
        final String GENERATION_COLLECTOR = "generation collector";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, GENERATION_COLLECTOR);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        String returned = extractor.getGenerationCollector(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(GENERATION_COLLECTOR, returned);
    }

    @Test
    public void testGenerationCollectorNone() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.collector.0.name";
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName(MONITOR_NAME)).thenReturn(null);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        String returned = extractor.getGenerationCollector(0);

        assertEquals(Generation.COLLECTOR_NONE, returned);
    }

    @Test
    public void testTotalSpaces() throws MonitorException {
        final Long TOTAL_SPACES = 99l;
        final LongMonitor monitor = mock(LongMonitor.class);
        when(monitor.getValue()).thenReturn(TOTAL_SPACES);
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName("sun.gc.generation.0.spaces")).thenReturn(monitor);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getTotalSpaces(0);

        verify(vm).findByName(eq("sun.gc.generation.0.spaces"));
        assertEquals(TOTAL_SPACES, returned);
    }


    @Test
    public void testSpaceName() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.name";
        final String SPACE_NAME = "Hilbert";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, SPACE_NAME);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        String returned = extractor.getSpaceName(0,0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(SPACE_NAME, returned);
    }

    @Test
    public void testSpaceCapacity() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.capacity";
        final Long SPACE_CAPACITY = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, SPACE_CAPACITY);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getSpaceCapacity(0,0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(SPACE_CAPACITY, returned);
    }

    @Test
    public void testSpaceMaxCapacity() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.maxCapacity";
        final Long SPACE_MAX_CAPACITY = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, SPACE_MAX_CAPACITY);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getSpaceMaxCapacity(0,0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(SPACE_MAX_CAPACITY, returned);
    }

    @Test
    public void testSpaceUsed() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.used";
        final Long SPACE_USED = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, SPACE_USED);

        VmMemoryDataExtractor extractor = new VmMemoryDataExtractor(vm);
        Long returned = extractor.getSpaceUsed(0,0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(SPACE_USED, returned);
    }

}
