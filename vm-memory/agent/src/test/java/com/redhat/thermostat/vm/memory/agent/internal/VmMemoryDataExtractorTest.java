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

package com.redhat.thermostat.vm.memory.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;

public class VmMemoryDataExtractorTest {

    private VmUpdate update;
    private VmMemoryDataExtractor extractor;

    @Before
    public void setup() {
        update = mock(VmUpdate.class);
        extractor = new VmMemoryDataExtractor(update);
    }
    
    @Test
    public void testTotalGcGenerations() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.policy.generations";
        final Long GC_GENERATIONS = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(GC_GENERATIONS);
        
        Long returned = extractor.getTotalGcGenerations();
        assertEquals(GC_GENERATIONS, returned);
    }

    @Test
    public void testGenerationName() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.name";
        final String GENERATION_NAME = "Youth";

        when(update.getPerformanceCounterString(eq(MONITOR_NAME))).thenReturn(GENERATION_NAME);

        String returned = extractor.getGenerationName(0);
        assertEquals(GENERATION_NAME, returned);
    }

    @Test
    public void testGenerationCapacity() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.capacity";
        final Long GENERATION_CAPACITY = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(GENERATION_CAPACITY);

        Long returned = extractor.getGenerationCapacity(0);
        assertEquals(GENERATION_CAPACITY, returned);
    }

    @Test
    public void testGenerationMaxCapacity() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.maxCapacity";
        final Long GENERATION_MAX_CAPACITY = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(GENERATION_MAX_CAPACITY);

        Long returned = extractor.getGenerationMaxCapacity(0);
        assertEquals(GENERATION_MAX_CAPACITY, returned);
    }

    @Test
    public void testGenerationCollector() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.collector.0.name";
        final String GENERATION_COLLECTOR = "generation collector";

        when(update.getPerformanceCounterString(eq(MONITOR_NAME))).thenReturn(GENERATION_COLLECTOR);

        String returned = extractor.getGenerationCollector(0);
        assertEquals(GENERATION_COLLECTOR, returned);
    }

    @Test
    public void testGenerationCollectorNone() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.collector.0.name";

        when(update.getPerformanceCounterString(eq(MONITOR_NAME))).thenReturn(null);

        String returned = extractor.getGenerationCollector(0);
        assertEquals(Generation.COLLECTOR_NONE, returned);
    }

    @Test
    public void testTotalSpaces() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.spaces";
        final Long TOTAL_SPACES = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(TOTAL_SPACES);

        Long returned = extractor.getTotalSpaces(0);
        assertEquals(TOTAL_SPACES, returned);
    }


    @Test
    public void testSpaceName() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.name";
        final String SPACE_NAME = "Hilbert";

        when(update.getPerformanceCounterString(eq(MONITOR_NAME))).thenReturn(SPACE_NAME);

        String returned = extractor.getSpaceName(0,0);
        assertEquals(SPACE_NAME, returned);
    }

    @Test
    public void testSpaceCapacity() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.capacity";
        final Long SPACE_CAPACITY = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(SPACE_CAPACITY);

        Long returned = extractor.getSpaceCapacity(0,0);
        assertEquals(SPACE_CAPACITY, returned);
    }

    @Test
    public void testSpaceMaxCapacity() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.maxCapacity";
        final Long SPACE_MAX_CAPACITY = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(SPACE_MAX_CAPACITY);

        Long returned = extractor.getSpaceMaxCapacity(0,0);
        assertEquals(SPACE_MAX_CAPACITY, returned);
    }

    @Test
    public void testSpaceUsed() throws VmUpdateException {
        final String MONITOR_NAME = "sun.gc.generation.0.space.0.used";
        final Long SPACE_USED = 99l;

        when(update.getPerformanceCounterLong(eq(MONITOR_NAME))).thenReturn(SPACE_USED);

        Long returned = extractor.getSpaceUsed(0,0);
        assertEquals(SPACE_USED, returned);
    }

}

