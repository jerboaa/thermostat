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

package com.redhat.thermostat.vm.gc.common;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import org.junit.Test;

import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;

public class GcCommonNameMapperTest {
    
    private static final GcCommonNameMapper mapper = new GcCommonNameMapper();

    @Test
    public void testMappings() {
        Set<String> distinctSet = getDistinctColNames("CMS", "PCopy");
        CollectorCommonName actual = mapper.mapToCommonName(distinctSet);
        assertEquals("'CMS' + 'PCopy' should map to concurrent collector",
                CollectorCommonName.CONCURRENT_COLLECTOR, actual);
        distinctSet = getDistinctColNames("PCopy", "MSC");
        actual = mapper.mapToCommonName(distinctSet);
        assertEquals("'MSC' + 'PCopy' should map to MS Compact GC",
                CollectorCommonName.MARK_SWEEP_COMPACT, actual);
        distinctSet = getDistinctColNames("G1 incremental collections");
        actual = mapper.mapToCommonName(distinctSet);
        assertEquals("'G1 incremental collections' should map to G1",
                CollectorCommonName.G1, actual);
        distinctSet = getDistinctColNames("PSParallelCompact", "PSScavenge");
        actual = mapper.mapToCommonName(distinctSet);
        assertEquals("'PSParallelCompact' + 'PSScavenge' should map to parallel collector",
                CollectorCommonName.PARALLEL_COLLECTOR, actual);
        distinctSet = getDistinctColNames("MSC", "Copy");
        actual = mapper.mapToCommonName(distinctSet);
        assertEquals("'MSC' + 'Copy' should map to serial collector",
                CollectorCommonName.SERIAL_COLLECTOR, actual);
    }
    
    @Test
    public void testHumanReadableNames() {
        assertEquals("Parallel Collector", CollectorCommonName.PARALLEL_COLLECTOR.getHumanReadableString());
        assertEquals("Serial Collector", CollectorCommonName.SERIAL_COLLECTOR.getHumanReadableString());
        assertEquals("Garbage-First Collector (G1)", CollectorCommonName.G1.getHumanReadableString());
        assertEquals("Concurrent Collector (Concurrent Mark and Sweep)", CollectorCommonName.CONCURRENT_COLLECTOR.getHumanReadableString());
        assertEquals("Mark Sweep Compact Collector", CollectorCommonName.MARK_SWEEP_COMPACT.getHumanReadableString());
        assertEquals("Unknown Collector", CollectorCommonName.UNKNOWN_COLLECTOR.getHumanReadableString());
    }
    
    @Test
    public void testUnknownCollector() {
        Set<String> distinctSet = getDistinctColNames("Me_Dont_Know_Sub-Collector");
        CollectorCommonName actual = mapper.mapToCommonName(distinctSet);
        assertEquals("Some random set should map to unknown collector",
                CollectorCommonName.UNKNOWN_COLLECTOR, actual);
        distinctSet = getDistinctColNames("foo", "bar");
        actual = mapper.mapToCommonName(distinctSet);
        assertEquals("Some random set should map to unknown collector",
                CollectorCommonName.UNKNOWN_COLLECTOR, actual);
    }
    
    private Set<String> getDistinctColNames(String ... vals) {
        Set<String> retval = new HashSet<>();
        for (String val: vals) {
            retval.add(val);
        }
        return retval;
    }
}
