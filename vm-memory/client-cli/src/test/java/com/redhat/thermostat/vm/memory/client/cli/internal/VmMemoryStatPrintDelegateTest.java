/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.cli.VMStatPrintDelegate;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class VmMemoryStatPrintDelegateTest {

    private VmMemoryStatDAO vmMemoryStatDAO;
    private VMStatPrintDelegate delegate;
    private VmRef vm;
    private List<VmMemoryStat> memoryStats;

    @Before
    public void setUp() {
        setupDAOs();
        delegate = new VmMemoryStatPrintDelegate(vmMemoryStatDAO);
    }

    @After
    public void tearDown() {
        vmMemoryStatDAO = null;
    }

    private void setupDAOs() {
        String vmId = "vmId";
        HostRef host = new HostRef("123", "dummy");
        vm = new VmRef(host, vmId, 234, "dummy");

        VmMemoryStat.Space space1_1_1 = newSpace("space1", 123456, 12345, 1, 0);
        VmMemoryStat.Space space1_1_2 = newSpace("space2", 123456, 12345, 1, 0);
        VmMemoryStat.Space[] spaces1_1 = new VmMemoryStat.Space[] { space1_1_1, space1_1_2 };
        VmMemoryStat.Generation gen1_1 = newGeneration("gen1", "col1", 123456, 12345, spaces1_1);

        VmMemoryStat.Space space1_2_1 = newSpace("space3", 123456, 12345, 1, 0);
        VmMemoryStat.Space space1_2_2 = newSpace("space4", 123456, 12345, 1, 0);
        VmMemoryStat.Space[] spaces1_2 = new VmMemoryStat.Space[] { space1_2_1, space1_2_2 };
        VmMemoryStat.Generation gen1_2 = newGeneration("gen2", "col1", 123456, 12345, spaces1_2);

        VmMemoryStat.Generation[] gens1 = new VmMemoryStat.Generation[] { gen1_1, gen1_2 };

        VmMemoryStat memStat1 = new VmMemoryStat("foo-agent", 1, vmId, gens1);

        VmMemoryStat.Space space2_1_1 = newSpace("space1", 123456, 12345, 2, 0);
        VmMemoryStat.Space space2_1_2 = newSpace("space2", 123456, 12345, 2, 0);
        VmMemoryStat.Space[] spaces2_1 = new VmMemoryStat.Space[] { space2_1_1, space2_1_2 };
        VmMemoryStat.Generation gen2_1 = newGeneration("gen1", "col1", 123456, 12345, spaces2_1);

        VmMemoryStat.Space space2_2_1 = newSpace("space3", 123456, 12345, 3, 0);
        VmMemoryStat.Space space2_2_2 = newSpace("space4", 123456, 12345, 4, 0);
        VmMemoryStat.Space[] spaces2_2 = new VmMemoryStat.Space[] { space2_2_1, space2_2_2 };
        VmMemoryStat.Generation gen2_2 = newGeneration("gen2", "col1", 123456, 12345, spaces2_2);

        VmMemoryStat.Generation[] gens2 = new VmMemoryStat.Generation[] { gen2_1, gen2_2 };

        VmMemoryStat memStat2 = new VmMemoryStat("foo-agent", 2, vmId, gens2);

        VmMemoryStat.Space space3_1_1 = newSpace("space1", 123456, 12345, 4, 0);
        VmMemoryStat.Space space3_1_2 = newSpace("space2", 123456, 12345, 5, 0);
        VmMemoryStat.Space[] spaces3_1 = new VmMemoryStat.Space[] { space3_1_1, space3_1_2 };
        VmMemoryStat.Generation gen3_1 = newGeneration("gen1", "col1", 123456, 12345, spaces3_1);

        VmMemoryStat.Space space3_2_1 = newSpace("space3", 123456, 12345, 6, 0);
        VmMemoryStat.Space space3_2_2 = newSpace("space4", 123456, 12345, 7, 0);
        VmMemoryStat.Space[] spaces3_2 = new VmMemoryStat.Space[] { space3_2_1, space3_2_2 };
        VmMemoryStat.Generation gen3_2 = newGeneration("gen2", "col1", 123456, 12345, spaces3_2);

        VmMemoryStat.Generation[] gens3 = new VmMemoryStat.Generation[] { gen3_1, gen3_2 };

        VmMemoryStat memStat3 = new VmMemoryStat("foo-agent", 3, vmId, gens3);

        vmMemoryStatDAO = mock(VmMemoryStatDAO.class);
        memoryStats = Arrays.asList(memStat1, memStat2, memStat3);
        when(vmMemoryStatDAO.getLatestVmMemoryStats(vm, Long.MIN_VALUE))
            .thenReturn(memoryStats);
    }

    private Space newSpace(String name, long maxCapacity, long capacity, long used, int index) {
        VmMemoryStat.Space space = new VmMemoryStat.Space();
        space.setName(name);
        space.setMaxCapacity(maxCapacity);
        space.setCapacity(capacity);
        space.setUsed(used);
        space.setIndex(index);
        return space;
    }

    private Generation newGeneration(String name, String collector, long maxCapacity, long capacity, Space[] spaces) {
        VmMemoryStat.Generation gen = new VmMemoryStat.Generation();
        gen.setName(name);
        gen.setCollector(collector);
        gen.setMaxCapacity(capacity);
        gen.setSpaces(spaces);
        return gen;
    }

    @Test
    public void testGetLatestStats() {
        List<? extends TimeStampedPojo> stats = delegate.getLatestStats(vm, Long.MIN_VALUE);
        assertEquals(memoryStats, stats);
    }
    
    @Test
    public void testGetHeaders() {
        List<String> headers = delegate.getHeaders(memoryStats.get(0));
        assertEquals(Arrays.asList("MEM.space1", "MEM.space2", "MEM.space3", "MEM.space4"), headers);
    }

    @Test
    public void testGetStatRow() throws CommandException {
        final List<String> row1 = Arrays.asList("1 B", "1 B", "1 B", "1 B");
        final List<String> row2 = Arrays.asList("2 B", "2 B", "3 B", "4 B");
        final List<String> row3 = Arrays.asList("4 B", "5 B", "6 B", "7 B");
        assertEquals(row1, delegate.getStatRow(memoryStats.get(0)));
        assertEquals(row2, delegate.getStatRow(memoryStats.get(1)));
        assertEquals(row3, delegate.getStatRow(memoryStats.get(2)));
    }

}

